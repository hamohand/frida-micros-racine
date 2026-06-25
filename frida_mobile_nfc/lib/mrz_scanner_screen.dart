import 'dart:io';
import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:flutter/foundation.dart';
import 'package:google_mlkit_text_recognition/google_mlkit_text_recognition.dart';
import 'nfc_reader_screen.dart';

class MrzScannerScreen extends StatefulWidget {
  final List<CameraDescription> cameras;

  const MrzScannerScreen({Key? key, required this.cameras}) : super(key: key);

  @override
  _MrzScannerScreenState createState() => _MrzScannerScreenState();
}

class _MrzScannerScreenState extends State<MrzScannerScreen> {
  late CameraController _controller;
  final TextRecognizer _textRecognizer = TextRecognizer(script: TextRecognitionScript.latin);
  bool _isProcessing = false;
  bool _isLocked = false;
  String _mrzResult = "Pointez la caméra vers la MRZ";

  @override
  void initState() {
    super.initState();
    // Initialize the camera
    _controller = CameraController(
      widget.cameras[0], 
      ResolutionPreset.high, 
      enableAudio: false,
      imageFormatGroup: Platform.isAndroid ? ImageFormatGroup.nv21 : ImageFormatGroup.bgra8888,
    );
    _controller.initialize().then((_) {
      if (!mounted) return;
      setState(() {});
      // Start image stream for ML Kit
      _controller.startImageStream(_processCameraImage);
    });
  }

  Future<void> _processCameraImage(CameraImage image) async {
    if (_isProcessing || _isLocked) return;
    _isProcessing = true;

    try {
      final WriteBuffer allBytes = WriteBuffer();
      for (final Plane plane in image.planes) {
        allBytes.putUint8List(plane.bytes);
      }
      final bytes = allBytes.done().buffer.asUint8List();

      final Size imageSize = Size(image.width.toDouble(), image.height.toDouble());
      final imageRotation = InputImageRotationValue.fromRawValue(widget.cameras[0].sensorOrientation) ?? InputImageRotation.rotation0deg;
      final inputImageFormat = InputImageFormatValue.fromRawValue(image.format.raw) ?? InputImageFormat.nv21;

      final inputImageData = InputImageMetadata(
        size: imageSize,
        rotation: imageRotation,
        format: inputImageFormat,
        bytesPerRow: image.planes[0].bytesPerRow,
      );

      final inputImage = InputImage.fromBytes(bytes: bytes, metadata: inputImageData);
      final RecognizedText recognizedText = await _textRecognizer.processImage(inputImage);

      // Heuristique MRZ stricte : les lignes MRZ ont exactement 30, 36 ou 44 caractères
      List<String> mrzLines = [];
      for (TextBlock block in recognizedText.blocks) {
        for (TextLine line in block.lines) {
          String text = line.text.replaceAll(' ', '');
          if (text.contains('<') && (text.length == 30 || text.length == 36 || text.length == 44)) {
            mrzLines.add(text);
          }
        }
      }

      // On n'accepte le résultat que s'il y a 2 ou 3 lignes de MRZ
      if (mrzLines.length >= 2) {
        // Optionnel : vérifier que toutes les lignes ont la même taille
        if (mrzLines[0].length == mrzLines[1].length) {
          String foundMrz = mrzLines.join('\n');
          if (foundMrz != _mrzResult) {
            setState(() {
              _mrzResult = foundMrz;
            });
          }
        }
      }
    } catch (e) {
      print("Erreur de traitement de l'image: $e");
    } finally {
      _isProcessing = false;
    }
  }

  @override
  void dispose() {
    _controller.stopImageStream();
    _controller.dispose();
    _textRecognizer.close();
    super.dispose();
  }

  void _validerScan() {
    setState(() {
      _isLocked = true;
    });
    _controller.stopImageStream();
    
    // Simuler le passage à l'étape NFC
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text("MRZ Capturée avec succès"),
          content: Text("Résultat :\n\n$_mrzResult\n\n(Ces données serviront de clé BAC pour déverrouiller la puce NFC)"),
          actions: [
            TextButton(
              child: const Text("Refaire le scan"),
              onPressed: () {
                Navigator.of(context).pop();
                setState(() {
                  _isLocked = false;
                  _mrzResult = "Pointez la caméra vers la MRZ";
                });
                _controller.startImageStream(_processCameraImage);
              },
            ),
            ElevatedButton(
              child: const Text("Passer au lecteur NFC"),
              onPressed: () {
                Navigator.of(context).pop();
                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (context) => NfcReaderScreen(mrzText: _mrzResult),
                  ),
                );
              },
            )
          ],
        );
      }
    );
  }

  @override
  Widget build(BuildContext context) {
    if (!_controller.value.isInitialized) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    return Scaffold(
      appBar: AppBar(title: const Text('Scanner MRZ')),
      body: Stack(
        fit: StackFit.expand,
        children: [
          CameraPreview(_controller),
          if (_isLocked)
            Container(color: Colors.black.withOpacity(0.5)), // Effet assombri quand bloqué
          Positioned(
            bottom: 80,
            left: 20,
            right: 20,
            child: Column(
              children: [
                Container(
                  padding: const EdgeInsets.all(15),
                  color: Colors.black87,
                  child: Text(
                    _mrzResult,
                    style: const TextStyle(color: Colors.white, fontSize: 16, fontFamily: 'monospace'),
                    textAlign: TextAlign.center,
                  ),
                ),
                const SizedBox(height: 20),
                if (!_isLocked && _mrzResult.contains('\n'))
                  ElevatedButton.icon(
                    icon: const Icon(Icons.check_circle),
                    label: const Text("Valider cette MRZ", style: TextStyle(fontSize: 18)),
                    style: ElevatedButton.styleFrom(
                      padding: const EdgeInsets.symmetric(horizontal: 30, vertical: 15),
                      backgroundColor: Colors.teal,
                      foregroundColor: Colors.white,
                    ),
                    onPressed: _validerScan,
                  ),
              ],
            ),
          )
        ],
      ),
    );
  }
}
