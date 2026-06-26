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
import kotlin.concurrent.thread

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
                        result.error("NFC_DISABLED", "NFC non disponible", null)
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
        runOnUiThread {
            try {
                val flags = NfcAdapter.FLAG_READER_NFC_A or 
                            NfcAdapter.FLAG_READER_NFC_B or 
                            NfcAdapter.FLAG_READER_NFC_F or 
                            NfcAdapter.FLAG_READER_NFC_V or 
                            NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
                
                val options = Bundle()
                options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)

                nfcAdapter?.enableReaderMode(
                    this@MainActivity,
                    this@MainActivity,
                    flags,
                    options
                )
                println("JMRTD: Mode lecteur NFC activé avec succès (ReaderMode + Delay 250ms).")
            } catch (e: Throwable) {
                println("JMRTD: Impossible d'activer le NFC: \${e.message}")
                pendingResult?.error("NFC_INIT_ERROR", "Impossible d'activer le NFC : \${e.message}", null)
                pendingResult = null
            }
        }
    }

    private fun disableNfcReaderMode() {
        isNfcScanActive = false
        runOnUiThread {
            try {
                nfcAdapter?.disableReaderMode(this@MainActivity)
                println("JMRTD: Mode lecteur NFC désactivé.")
            } catch (e: Throwable) { }
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
        println("JMRTD: TAG NFC DETECTE !!")
        
        val vibrator = getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(150, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(150)
        }

        val isoDep = IsoDep.get(tag)
        if (isoDep == null) {
            println("JMRTD: Le tag détecté n'est pas IsoDep.")
            runOnUiThread {
                pendingResult?.error("NFC_ERROR", "La carte détectée n'est pas compatible (IsoDep manquant)", null)
                pendingResult = null
                disableNfcReaderMode()
            }
            return
        }

        thread {
            var cardService: CardService? = null
            try {
                println("JMRTD: Démarrage JMRTD...")
                isoDep.timeout = 15000 // 15 secondes max
                cardService = CardService.getInstance(isoDep)
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
                    try {
                        println("JMRTD: Authentification BAC en cours...")
                        passportService.doBAC(key)
                        println("JMRTD: BAC réussi ! Lecture DG1...")
                    } catch (bacEx: Throwable) {
                        println("JMRTD: BAC a échoué (\${bacEx.message}). Tentative de repli vers PACE (SAC)...")
                        try {
                            val cardAccessIn = passportService.getInputStream(PassportService.EF_CARD_ACCESS)
                            val cardAccessFile = org.jmrtd.lds.CardAccessFile(cardAccessIn)
                            val securityInfos = cardAccessFile.securityInfos
                            var paceSucceeded = false

                            if (securityInfos != null) {
                                for (securityInfo in securityInfos) {
                                    if (securityInfo is org.jmrtd.lds.PACEInfo) {
                                        println("JMRTD: Informations PACE trouvées, lancement de PACE...")
                                        val paceKey = org.jmrtd.PACEKeySpec.createMRZKey(key)
                                        passportService.doPACE(
                                            paceKey,
                                            securityInfo.objectIdentifier,
                                            org.jmrtd.lds.PACEInfo.toParameterSpec(securityInfo.parameterId),
                                            securityInfo.parameterId
                                        )
                                        println("JMRTD: PACE réussi ! Lecture DG1...")
                                        paceSucceeded = true
                                        break
                                    }
                                }
                            }
                            if (!paceSucceeded) {
                                throw Exception("Fichier CardAccess introuvable ou aucun protocole PACE supporté.")
                            }
                        } catch (paceEx: Throwable) {
                            println("JMRTD: PACE a également échoué (\${paceEx.message}).")
                            throw bacEx // On renvoie l'erreur originale BAC pour ne pas masquer le problème de MRZ
                        }
                    }
                    
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

                    println("JMRTD: Lecture réussie. Retour à Flutter.")
                    runOnUiThread {
                        disableNfcReaderMode()
                        pendingResult?.success(jsonResult)
                        pendingResult = null
                    }
                }
            } catch (e: Throwable) {
                println("JMRTD: Erreur JMRTD: ${e.message}")
                e.printStackTrace()
                runOnUiThread {
                    disableNfcReaderMode()
                    pendingResult?.error("NFC_ERROR", e.toString(), null)
                    pendingResult = null
                }
            } finally {
                try { cardService?.close() } catch (e: Throwable) { }
                try { isoDep.close() } catch (e: Throwable) { }
            }
        }
    }
}
