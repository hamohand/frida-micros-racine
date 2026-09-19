import 'package:image/image.dart' as img;
import 'dart:io';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:camera/camera.dart';
import 'package:http/http.dart' as http;
import 'nfc_reader_screen.dart';

class IdFrontScannerScreen extends StatefulWidget {
  final List<CameraDescription> cameras;
  final String? docNumber;
  final String? dob;
  final String? exp;
  final String? mrzText;
  final String uploadUrl;

  const IdFrontScannerScreen({
    super.key,
    required this.cameras,
    this.docNumber,
    this.dob,
    this.exp,
    this.mrzText,
    required this.uploadUrl,
  });

  @override
  State<IdFrontScannerScreen> createState() => _IdFrontScannerScreenState();
}

class _IdFrontScannerScreenState extends State<IdFrontScannerScreen> {
  late CameraController _controller;
  bool _isCameraInitialized = false;
  bool _isScanning = false;
  String _statusMessage = 'Cadrez le RECTO de la carte';

  @override
  void initState() {
    super.initState();
    _initializeCamera();
  }

  void _initializeCamera() async {
    // Petit délai pour s'assurer que l'écran précédent a bien libéré la caméra
    await Future.delayed(const Duration(milliseconds: 600));
    final camera = widget.cameras.firstWhere(
      (c) => c.lensDirection == CameraLensDirection.back,
      orElse: () => widget.cameras.first,
    );

    _controller = CameraController(
      camera,
      ResolutionPreset.high,
      enableAudio: false,
      imageFormatGroup: Platform.isAndroid ? ImageFormatGroup.nv21 : ImageFormatGroup.bgra8888,
    );

    await _controller.initialize();
    if (!mounted) return;
    setState(() {
      _isCameraInitialized = true;
    });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _captureAndScan() async {
    if (_isScanning || !_controller.value.isInitialized) return;

    setState(() {
      _isScanning = true;
      _statusMessage = 'Capture en cours...';
    });

    try {
      print('📸 1. Prise de photo...');
      final imageFile = await _controller.takePicture();
      var bytes = await imageFile.readAsBytes();
      print('📸 2. Photo prise: ${bytes.length} bytes');
      
      // Decodage et recadrage sur le rectangle central
      setState(() { _statusMessage = 'Recadrage...'; });
      
      img.Image? capturedImage = img.decodeImage(bytes);
      if (capturedImage != null) {
        print('📸 3. Image décodée: ${capturedImage.width}x${capturedImage.height}');
        int cropWidth = (capturedImage.width * 0.85).toInt();
        int cropHeight = (cropWidth * (200 / 300)).toInt();
        
        int cropX = (capturedImage.width - cropWidth) ~/ 2;
        int cropY = (capturedImage.height - cropHeight) ~/ 2;
        
        img.Image cropped = img.copyCrop(capturedImage, x: cropX, y: cropY, width: cropWidth, height: cropHeight);
        bytes = img.encodeJpg(cropped, quality: 90);
        print('📸 4. Image recadrée: ${bytes.length} bytes');
      }
      
      final base64Image = base64Encode(bytes);

      final uri = Uri.parse(widget.uploadUrl);
      final baseUrl = '${uri.scheme}://${uri.host}:${uri.port}/api';
      final targetUrl = '$baseUrl/pdfs/ocr-cni-front';
      
      // Extraire le sessionId de l'uploadUrl
      final segments = uri.pathSegments;
      String sessionId = '';
      for (int i = 0; i < segments.length - 1; i++) {
        if (segments[i] == 'nfc-session') {
          sessionId = segments[i + 1];
          break;
        }
      }
      
      print('📡 5. Envoi OCR: $targetUrl (session: $sessionId, ${(base64Image.length / 1024).toStringAsFixed(0)} Ko)');
      setState(() {
        _statusMessage = 'Envoi OCR (${(base64Image.length / 1024).toStringAsFixed(0)} Ko)...';
      });

      final response = await http.post(
        Uri.parse(targetUrl),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'image': base64Image, 'sessionId': sessionId}),
      ).timeout(const Duration(seconds: 120));

      print('📡 6. Réponse OCR: ${response.statusCode}');
      
      if (response.statusCode == 200) {
        print('✅ 7. OCR OK, passage au NFC');
        _goToNfcScreen();
      } else {
        print('❌ 7. OCR échoué: ${response.statusCode} ${response.body}');
        _showErrorAndProceed('OCR échoué (HTTP ${response.statusCode})');
      }
    } catch (e) {
      print('❌ ERREUR: $e');
      setState(() {
        _statusMessage = 'Erreur: $e';
        _isScanning = false;
      });
      await Future.delayed(const Duration(seconds: 3));
      if (mounted) _goToNfcScreen();
    }
  }

  void _showErrorAndProceed(String msg) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
    _goToNfcScreen();
  }

  void _goToNfcScreen({String? nomArabe, String? prenomArabe}) {
    if (!mounted) return;
    Navigator.pushReplacement(
      context,
      MaterialPageRoute(
        builder: (context) => NfcReaderScreen(
          docNumber: widget.docNumber,
          dob: widget.dob,
          exp: widget.exp,
          mrzText: widget.mrzText,
          cameras: widget.cameras,
          uploadUrl: widget.uploadUrl,
          nomArabe: nomArabe,
          prenomArabe: prenomArabe,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    if (!_isCameraInitialized) {
      return const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      );
    }

    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        title: const Text('Capture du Recto'),
        backgroundColor: Colors.black,
      ),
      body: Stack(
        fit: StackFit.expand,
        children: [
          CameraPreview(_controller),
          
          // Overlay simple
          Center(
            child: Container(
              width: 300,
              height: 200,
              decoration: BoxDecoration(
                border: Border.all(color: Colors.greenAccent, width: 3.0),
                borderRadius: BorderRadius.circular(12.0),
              ),
            ),
          ),
          
          Positioned(
            bottom: 40,
            left: 0,
            right: 0,
            child: Column(
              children: [
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.black87,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(
                    _statusMessage,
                    style: const TextStyle(color: Colors.white, fontSize: 16),
                  ),
                ),
                const SizedBox(height: 20),
                FloatingActionButton(
                  onPressed: _isScanning ? null : _captureAndScan,
                  backgroundColor: _isScanning ? Colors.grey : Colors.greenAccent,
                  child: _isScanning
                      ? const CircularProgressIndicator(color: Colors.white)
                      : const Icon(Icons.camera_alt, color: Colors.black),
                ),
                const SizedBox(height: 10),
                TextButton(
                  onPressed: _isScanning ? null : () => _goToNfcScreen(),
                  child: const Text('Ignorer (Passer au NFC)', style: TextStyle(color: Colors.white70)),
                ),
              ],
            ),
          )
        ],
      ),
    );
  }
}

class _ScannerOverlayShape extends ShapeBorder {
  final Color borderColor;
  final double borderWidth;
  final Color overlayColor;

  _ScannerOverlayShape({
    this.borderColor = Colors.white,
    this.borderWidth = 1.0,
    this.overlayColor = const Color(0x88000000),
  });

  @override
  EdgeInsetsGeometry get dimensions => const EdgeInsets.all(10.0);

  @override
  Path getInnerPath(Rect rect, {TextDirection? textDirection}) {
    return Path()
      ..fillType = PathFillType.evenOdd
      ..addPath(getOuterPath(rect), Offset.zero);
  }

  @override
  Path getOuterPath(Rect rect, {TextDirection? textDirection}) {
    Path _getLeftTopPath(Rect rect) {
      return Path()
        ..moveTo(rect.left, rect.bottom)
        ..lineTo(rect.left, rect.top)
        ..lineTo(rect.right, rect.top);
    }
    return _getLeftTopPath(rect)
      ..lineTo(rect.right, rect.bottom)
      ..lineTo(rect.left, rect.bottom)
      ..close();
  }

  @override
  void paint(Canvas canvas, Rect rect, {TextDirection? textDirection}) {
    const width = 300.0;
    const height = 200.0;
    final scanArea = Rect.fromCenter(
      center: rect.center,
      width: width,
      height: height,
    );

    final backgroundPaint = Paint()
      ..color = overlayColor
      ..style = PaintingStyle.fill;
    
    final borderPaint = Paint()
      ..color = borderColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = borderWidth;

    final backgroundPath = Path()
      ..addRect(rect)
      ..addRRect(RRect.fromRectAndRadius(scanArea, const Radius.circular(12)))
      ..fillType = PathFillType.evenOdd;
    
    canvas.drawPath(backgroundPath, backgroundPaint);
    canvas.drawRRect(RRect.fromRectAndRadius(scanArea, const Radius.circular(12)), borderPaint);
  }

  @override
  ShapeBorder scale(double t) {
    return _ScannerOverlayShape(
      borderColor: borderColor,
      borderWidth: borderWidth * t,
      overlayColor: overlayColor,
    );
  }
}








