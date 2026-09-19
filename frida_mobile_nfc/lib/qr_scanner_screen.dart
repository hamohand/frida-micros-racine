import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:camera/camera.dart';
import 'id_front_scanner_screen.dart';
import 'mrz_scanner_screen.dart';
import 'nfc_reader_screen.dart';

class QRScannerScreen extends StatefulWidget {
  final List<CameraDescription> cameras;

  const QRScannerScreen({Key? key, required this.cameras}) : super(key: key);

  @override
  State<QRScannerScreen> createState() => _QRScannerScreenState();
}

class _QRScannerScreenState extends State<QRScannerScreen> {
  final MobileScannerController controller = MobileScannerController(
    detectionSpeed: DetectionSpeed.normal,
    facing: CameraFacing.back,
  );

  bool _isProcessing = false;

  void _onDetect(BarcodeCapture capture) {
    if (_isProcessing) return;
    final List<Barcode> barcodes = capture.barcodes;
    if (barcodes.isNotEmpty) {
      final barcode = barcodes.first;
      if (barcode.rawValue != null) {
        try {
          final data = jsonDecode(barcode.rawValue!);
          if (data['action'] == 'nfc_upload' && data['url'] != null) {
            _isProcessing = true;
            controller.stop();
            
            // Si le Web/Desktop a déjà lu la MRZ avec la webcam, il la passe dans le QR code !
            if (data['mrz'] != null && data['mrz']['doc'] != null) {
              Navigator.pushReplacement(
                context,
                MaterialPageRoute(
                  builder: (context) => IdFrontScannerScreen(
                    docNumber: data['mrz']['doc'],
                    dob: data['mrz']['dob'],
                    exp: data['mrz']['exp'],
                    cameras: widget.cameras,
                    uploadUrl: data['url'],
                  ),
                ),
              );
            } else {
              // Mode autonome classique : le mobile doit lui-même lire la MRZ avec sa caméra
              Navigator.pushReplacement(
                context,
                MaterialPageRoute(
                  builder: (context) => MrzScannerScreen(
                    cameras: widget.cameras,
                    uploadUrl: data['url'],
                  ),
                ),
              );
            }
          }
        } catch (e) {
          // Ce n'est pas notre QR Code
        }
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Connecter au Bureau'),
        backgroundColor: Colors.teal,
      ),
      body: Stack(
        children: [
          MobileScanner(
            controller: controller,
            onDetect: _onDetect,
          ),
          Center(
            child: Container(
              width: 250,
              height: 250,
              decoration: BoxDecoration(
                border: Border.all(color: Colors.tealAccent, width: 4),
                borderRadius: BorderRadius.circular(12),
              ),
            ),
          ),
          Positioned(
            bottom: 50,
            left: 20,
            right: 20,
            child: Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.black87,
                borderRadius: BorderRadius.circular(8),
              ),
              child: const Text(
                'Scannez le QR Code affiché sur l\'écran de votre ordinateur',
                style: TextStyle(color: Colors.white, fontSize: 16),
                textAlign: TextAlign.center,
              ),
            ),
          )
        ],
      ),
    );
  }

  @override
  void dispose() {
    controller.dispose();
    super.dispose();
  }
}
