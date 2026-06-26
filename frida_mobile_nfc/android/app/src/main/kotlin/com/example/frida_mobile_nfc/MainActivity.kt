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

class MainActivity : FlutterActivity(), NfcAdapter.ReaderCallback {

    private val CHANNEL = "frida.nfc/jmrtd"
    private var pendingResult: MethodChannel.Result? = null
    private var bacKey: BACKeySpec? = null
    private var nfcAdapter: NfcAdapter? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            if (call.method == "startNfcRead") {
                val docNum = call.argument<String>("documentNumber")
                val dob = call.argument<String>("dateOfBirth")
                val expiry = call.argument<String>("dateOfExpiry")
                
                if (docNum != null && dob != null && expiry != null) {
                    bacKey = BACKey(docNum, dob, expiry)
                    pendingResult = result
                    
                    // Récupérer l'adaptateur ici, quand le contexte est garanti d'être prêt
                    val adapter = NfcAdapter.getDefaultAdapter(this@MainActivity)
                    if (adapter == null) {
                        result.error("NFC_DISABLED", "NFC non disponible ou désactivé sur l'appareil", null)
                        return@setMethodCallHandler
                    }
                    nfcAdapter = adapter

                    // Activer le mode lecteur NFC du téléphone
                    try {
                        nfcAdapter?.enableReaderMode(
                            this@MainActivity,
                            this@MainActivity,
                            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                            null
                        )
                    } catch (e: Throwable) {
                        result.error("NFC_INIT_ERROR", "Impossible d'activer le NFC : ${e.toString()}", null)
                    }
                } else {
                    result.error("INVALID_ARGS", "Missing BAC keys", null)
                }
            } else {
                result.notImplemented()
            }
        }
    }

    override fun onTagDiscovered(tag: Tag?) {
        val isoDep = IsoDep.get(tag) ?: return
        try {
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
                // 1. Authentification BAC
                passportService.doBAC(key)
                
                // 2. Lecture du fichier DG1 (MRZ Data)
                val dg1In: InputStream = passportService.getInputStream(PassportService.EF_DG1)
                val dg1File = DG1File(dg1In)
                val mrzInfo = dg1File.mrzInfo

                // 3. Extraction du JSON
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

                runOnUiThread {
                    nfcAdapter?.disableReaderMode(this)
                    pendingResult?.success(jsonResult)
                    pendingResult = null
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
            runOnUiThread {
                nfcAdapter?.disableReaderMode(this)
                pendingResult?.error("NFC_ERROR", e.toString(), null)
                pendingResult = null
            }
        }
    }
}
