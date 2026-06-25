import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:google_mlkit_text_recognition/google_mlkit_text_recognition.dart';

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
  String _mrzResult = "Pointez la caméra vers la MRZ";

  @override
  void initState() {
    super.initState();
    // Initialize the camera
    _controller = CameraController(widget.cameras[0], ResolutionPreset.high, enableAudio: false);
    _controller.initialize().then((_) {
      if (!mounted) return;
      setState(() {});
      // Start image stream for ML Kit
      _controller.startImageStream(_processCameraImage);
    });
  }

  Future<void> _processCameraImage(CameraImage image) async {
    if (_isProcessing) return;
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

      // Simple MRZ heuristic: look for lines with multiple '<<'
      String foundMrz = "";
      for (TextBlock block in recognizedText.blocks) {
        for (TextLine line in block.lines) {
          if (line.text.contains("<<")) {
            foundMrz += line.text.replaceAll(' ', '') + "\n";
          }
        }
      }

      if (foundMrz.isNotEmpty && foundMrz != _mrzResult) {
        setState(() {
          _mrzResult = foundMrz;
        });
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
          Positioned(
            bottom: 50,
            left: 20,
            right: 20,
            child: Container(
              padding: const EdgeInsets.all(15),
              color: Colors.black87,
              child: Text(
                _mrzResult,
                style: const TextStyle(color: Colors.white, fontSize: 16, fontFamily: 'monospace'),
                textAlign: TextAlign.center,
              ),
            ),
          )
        ],
      ),
    );
  }
}
