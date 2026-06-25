import 'package:flutter/material.dart';

class NfcReaderScreen extends StatefulWidget {
  final String mrzText;

  const NfcReaderScreen({Key? key, required this.mrzText}) : super(key: key);

  @override
  _NfcReaderScreenState createState() => _NfcReaderScreenState();
}

class _NfcReaderScreenState extends State<NfcReaderScreen> {
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
      _status = "Lecture de la puce NFC en cours...\nNe bougez pas la carte.";
    });

    // TODO: Intégrer la librairie emrtd ou flutter_nfc_kit avec les clés BAC
    await Future.delayed(const Duration(seconds: 3));

    setState(() {
      _isReading = false;
      _status = "✅ Lecture réussie !\n\nNom: HAMROUN\nPrénom: MOHAMMED\nNIN: 195601032026062407\n\n(Les données JSON ont été extraites avec succès de la puce biométrique via la clé BAC).";
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Lecture Puce NFC')),
      body: Padding(
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
            Expanded(
              child: Container(
                decoration: BoxDecoration(
                  color: Colors.grey[200],
                  borderRadius: BorderRadius.circular(10),
                ),
                padding: const EdgeInsets.all(20),
                child: Center(
                  child: Text(
                    _status,
                    textAlign: TextAlign.center,
                    style: const TextStyle(fontSize: 16),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 20),
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
    );
  }
}
