import 'package:flutter/material.dart';
import 'package:emrtd/emrtd.dart';

class NfcReaderScreen extends StatefulWidget {
  final String mrzText;

  const NfcReaderScreen({Key? key, required this.mrzText}) : super(key: key);

  @override
  _NfcReaderScreenState createState() => _NfcReaderScreenState();
}

class _NfcReaderScreenState extends State<NfcReaderScreen> {
  final _emrtd = Emrtd();
  String _documentNumber = "";
  String _dateOfBirth = "";
  String _dateOfExpiry = "";
  String _status = "En attente du scan NFC...";
  bool _isReading = false;

  @override
  void initState() {
    super.initState();
    _parseMrzForBac(widget.mrzText);
  }

  void _parseMrzForBac(String mrz) {
    // Parser basique pour extraire les clés BAC (Document Number, DoB, Expiry)
    // C'est un POC : on nettoie les espaces et on cherche dans les lignes
    List<String> lines = mrz.split('\n').map((e) => e.trim()).where((e) => e.isNotEmpty).toList();
    
    if (lines.isNotEmpty) {
      if (lines.length == 3 && lines[0].length >= 30) {
        // TD1 (Ex: CNI Algérienne)
        _documentNumber = lines[0].substring(5, 14).replaceAll('<', '');
        _dateOfBirth = lines[1].substring(0, 6);
        _dateOfExpiry = lines[1].substring(8, 14);
      } else if (lines.length == 2 && lines[1].length >= 44) {
        // TD3 (Ex: Passeport)
        _documentNumber = lines[1].substring(0, 9).replaceAll('<', '');
        _dateOfBirth = lines[1].substring(13, 19);
        _dateOfExpiry = lines[1].substring(21, 27);
      }
    }
  }

  Future<void> _startNfcRead() async {
    setState(() {
      _isReading = true;
      _status = "Lecture de la puce NFC en cours...\nPlaquez la carte contre le dos de votre téléphone.";
    });

    try {
      final result = await _emrtd.readAndVerify(
        clientId: 'frida_poc_client',
        validationUri: 'wss://docval.kurzdigital.com/ws2/validate',
        validationId: 'session-${DateTime.now().millisecondsSinceEpoch}',
        documentNumber: _documentNumber,
        dateOfBirth: _dateOfBirth,
        dateOfExpiry: _dateOfExpiry,
      );

      setState(() {
        _isReading = false;
        _status = "✅ Lecture terminée !\n\nRésultat brut du serveur de validation:\n$result";
      });
    } catch (e) {
      setState(() {
        _isReading = false;
        _status = "❌ Erreur de lecture NFC :\n$e\n\n(Vérifiez que la carte est bien plaquée et que les clés BAC sont exactes).";
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Lecture Puce NFC')),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text("Clés BAC extraites de la MRZ :", style: TextStyle(fontWeight: FontWeight.bold)),
              const SizedBox(height: 10),
              Text("Numéro de Document : $_documentNumber"),
              Text("Date de Naissance : $_dateOfBirth"),
              Text("Date d'Expiration : $_dateOfExpiry"),
              const Divider(height: 40),
              Container(
                decoration: BoxDecoration(
                  color: Colors.grey[200],
                  borderRadius: BorderRadius.circular(10),
                ),
                padding: const EdgeInsets.all(20),
                constraints: const BoxConstraints(minHeight: 150),
                child: Center(
                  child: Text(
                    _status,
                    textAlign: TextAlign.center,
                    style: const TextStyle(fontSize: 16),
                  ),
                ),
              ),
              const SizedBox(height: 30),
              ElevatedButton.icon(
                icon: _isReading ? const CircularProgressIndicator(color: Colors.white) : const Icon(Icons.nfc),
                label: Text(_isReading ? "Lecture..." : "Lancer le scan NFC"),
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.all(15),
                  backgroundColor: Colors.teal,
                  foregroundColor: Colors.white,
                ),
                onPressed: _isReading ? null : _startNfcRead,
              )
            ],
          ),
        ),
      ),
    );
  }
}
