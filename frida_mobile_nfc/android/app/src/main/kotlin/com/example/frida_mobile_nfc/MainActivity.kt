import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
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

class MainActivity : FlutterActivity() {

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
                    enableNfcForegroundDispatch()
                } else {
                    result.error("INVALID_ARGS", "Missing BAC keys", null)
                }
            } else {
                result.notImplemented()
            }
        }
    }

    private fun enableNfcForegroundDispatch() {
        runOnUiThread {
            try {
                val intent = Intent(this, javaClass).apply {
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                val pendingIntent = PendingIntent.getActivity(
                    this, 0, intent,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                
                val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED))
                val techLists = arrayOf(arrayOf(IsoDep::class.java.name))

                nfcAdapter?.enableForegroundDispatch(this, pendingIntent, filters, techLists)
                println("JMRTD: Mode Foreground Dispatch activé avec succès.")
            } catch (e: Throwable) {
                println("JMRTD: Impossible d'activer le NFC: \${e.message}")
                pendingResult?.error("NFC_INIT_ERROR", "Erreur NFC : \${e.message}", null)
                pendingResult = null
            }
        }
    }

    private fun disableNfcForegroundDispatch() {
        isNfcScanActive = false
        runOnUiThread {
            try {
                nfcAdapter?.disableForegroundDispatch(this)
                println("JMRTD: Mode Foreground Dispatch désactivé.")
            } catch (e: Throwable) { }
        }
    }

    override fun onResume() {
        super.onResume()
        if (isNfcScanActive) {
            enableNfcForegroundDispatch()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isNfcScanActive) {
            try {
                nfcAdapter?.disableForegroundDispatch(this)
            } catch (e: Throwable) { }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (isNfcScanActive && (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action || NfcAdapter.ACTION_TAG_DISCOVERED == intent.action)) {
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            if (tag != null) {
                processNfcTag(tag)
            }
        }
    }

    private fun processNfcTag(tag: Tag) {
        println("JMRTD: TAG NFC DETECTE via Intent !!")
        
        val vibrator = getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(150, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(150)
        }

        val isoDep = IsoDep.get(tag)
        if (isoDep == null) {
            println("JMRTD: Le tag n'est pas IsoDep.")
            runOnUiThread {
                pendingResult?.error("NFC_ERROR", "La carte détectée n'est pas compatible", null)
                pendingResult = null
                disableNfcForegroundDispatch()
            }
            return
        }

        // Run network/crypto ops on a background thread
        thread {
            var cardService: CardService? = null
            try {
                println("JMRTD: Démarrage JMRTD...")
                isoDep.timeout = 15000 
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
                    println("JMRTD: Authentification BAC en cours...")
                    passportService.doBAC(key)
                    println("JMRTD: BAC réussi ! Lecture DG1...")
                    
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

                    println("JMRTD: Lecture réussie.")
                    runOnUiThread {
                        disableNfcForegroundDispatch()
                        pendingResult?.success(jsonResult)
                        pendingResult = null
                    }
                }
            } catch (e: Throwable) {
                println("JMRTD: Erreur: ${e.message}")
                runOnUiThread {
                    disableNfcForegroundDispatch()
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
