package com.example.frida_mobile_nfc

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import net.sf.scuba.smartcards.CardService
import org.jmrtd.BACKey
import org.jmrtd.BACKeySpec
import org.jmrtd.PassportService
import org.jmrtd.lds.icao.DG1File
import java.io.InputStream
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class MainActivity : FlutterActivity(), NfcAdapter.ReaderCallback {

    private val CHANNEL = "frida.nfc/jmrtd"
    private var pendingResult: MethodChannel.Result? = null
    private var bacKey: BACKeySpec? = null
    private var nfcAdapter: NfcAdapter? = null
    private var isNfcScanActive = false

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        try {
            Security.addProvider(BouncyCastleProvider())
        } catch (e: Throwable) {
            android.util.Log.e("JMRTD", "Erreur init BouncyCastle: \${e.message}")
        }

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "startNfcRead") {
                val docNum = call.argument<String>("documentNumber")
                val dob = call.argument<String>("dateOfBirth")
                val expiry = call.argument<String>("dateOfExpiry")
                
                if (docNum != null && dob != null && expiry != null) {
                    bacKey = BACKey(docNum, dob, expiry)
                    pendingResult = result
                    
                    val adapter = NfcAdapter.getDefaultAdapter(this@MainActivity)
                    if (adapter == null) {
                        result.error("NFC_DISABLED", "NFC non disponible ou désactivé sur l'appareil", null)
                        return@setMethodCallHandler
                    }
                    nfcAdapter = adapter
                    isNfcScanActive = true
                    enableNfcReaderMode()
                } else {
                    result.error("INVALID_ARGS", "Missing BAC keys", null)
                }
            } else {
                result.notImplemented()
            }
        }
    }

    private fun enableNfcReaderMode() {
        try {
            val options = Bundle()
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
            nfcAdapter?.enableReaderMode(
                this@MainActivity,
                this@MainActivity,
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                options
            )
            android.util.Log.d("JMRTD", "Mode lecteur NFC activé avec succès.")
        } catch (e: Throwable) {
            android.util.Log.e("JMRTD", "Impossible d'activer le NFC : \${e.message}")
            pendingResult?.error("NFC_INIT_ERROR", "Impossible d'activer le NFC : \${e.message}", null)
            pendingResult = null
        }
    }

    private fun disableNfcReaderMode() {
        isNfcScanActive = false
        try {
            nfcAdapter?.disableReaderMode(this@MainActivity)
            android.util.Log.d("JMRTD", "Mode lecteur NFC désactivé.")
        } catch (e: Throwable) {
            // Ignorer
        }
    }

    override fun onResume() {
        super.onResume()
        if (isNfcScanActive) {
            enableNfcReaderMode()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isNfcScanActive) {
            try {
                nfcAdapter?.disableReaderMode(this@MainActivity)
            } catch (e: Throwable) { }
        }
    }

    override fun onTagDiscovered(tag: Tag?) {
        android.util.Log.d("JMRTD", "TAG NFC DETECTE !!")
        
        // Faire vibrer le téléphone pour confirmer la détection physique
        val vibrator = getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(150, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(150)
        }

        val isoDep = IsoDep.get(tag)
        if (isoDep == null) {
            android.util.Log.e("JMRTD", "Le tag détecté n'est pas de type IsoDep (Carte incompatible)")
            runOnUiThread {
                pendingResult?.error("NFC_ERROR", "La carte détectée n'est pas compatible (IsoDep manquant)", null)
                pendingResult = null
                disableNfcReaderMode()
            }
            return
        }

        try {
            android.util.Log.d("JMRTD", "Démarrage JMRTD...")
            isoDep.timeout = 15000 // 15 secondes max
            val cardService = CardService.getInstance(isoDep)
            cardService.open()

            val passportService = PassportService(
                cardService,
                PassportService.NORMAL_MAX_TRANCEIVE_LENGTH,
                PassportService.DEFAULT_MAX_BLOCKSIZE,
                false,
                false
            )

            passportService.open()
            
            val key = bacKey
            if (key != null) {
                android.util.Log.d("JMRTD", "Authentification BAC en cours...")
                passportService.doBAC(key)
                android.util.Log.d("JMRTD", "BAC réussi ! Lecture DG1...")
                
                val dg1In: InputStream = passportService.getInputStream(PassportService.EF_DG1)
                val dg1File = DG1File(dg1In)
                val mrzInfo = dg1File.mrzInfo

                val jsonResult = """
                {
                    "documentCode": "${mrzInfo.documentCode}",
                    "issuingState": "${mrzInfo.issuingState}",
                    "primaryIdentifier": "${mrzInfo.primaryIdentifier?.replace("<", "")}",
                    "secondaryIdentifier": "${mrzInfo.secondaryIdentifier?.replace("<", "")}",
                    "nationality": "${mrzInfo.nationality}",
                    "documentNumber": "${mrzInfo.documentNumber}",
                    "dateOfBirth": "${mrzInfo.dateOfBirth}",
                    "gender": "${mrzInfo.gender}",
                    "dateOfExpiry": "${mrzInfo.dateOfExpiry}",
                    "personalNumber": "${mrzInfo.optionalData1}"
                }
                """.trimIndent()

                android.util.Log.d("JMRTD", "Lecture réussie. Retour à Flutter.")
                runOnUiThread {
                    disableNfcReaderMode()
                    pendingResult?.success(jsonResult)
                    pendingResult = null
                }
            }
        } catch (e: Throwable) {
            android.util.Log.e("JMRTD", "Erreur JMRTD: ${e.message}", e)
            e.printStackTrace()
            runOnUiThread {
                disableNfcReaderMode()
                pendingResult?.error("NFC_ERROR", e.toString(), null)
                pendingResult = null
            }
        }
    }
}
