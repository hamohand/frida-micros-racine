import 'dart:io';
import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'package:google_mlkit_text_recognition/google_mlkit_text_recognition.dart';
import 'nfc_reader_screen.dart';
import 'id_front_scanner_screen.dart';

class MrzScannerScreen extends StatefulWidget {
  final List<CameraDescription> cameras;
  final String uploadUrl;

  const MrzScannerScreen({Key? key, required this.cameras, required this.uploadUrl}) : super(key: key);

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
          String text = line.text.replaceAll(' ', '').toUpperCase();
          // Correction classique OCR : ML Kit confond souvent '<<' avec 'K', 'C', 'E' ou des parenthèses
          text = text.replaceAll('«', '<').replaceAll('(', '<').replaceAll(')', '<').replaceAll('[', '<').replaceAll(']', '<');
          text = text.replaceAllMapped(RegExp(r'(?<=<)[KCE]+(?=<)'), (m) => '<' * m.group(0)!.length);
          text = text.replaceAllMapped(RegExp(r'(?<=<)[KCE]+$'), (m) => '<' * m.group(0)!.length);
          text = text.replaceAllMapped(RegExp(r'^[KCE]+(?=<)'), (m) => '<' * m.group(0)!.length);
          
          // Tolérance pour la coupure des '<' à la fin de la ligne (ML Kit les ignore souvent)
          // Sur la première ligne (qui commence par ID, I<, P<, etc), MLKit ignore TOUS les chevrons de fin !
          if (text.startsWith(RegExp(r'^(ID|I<|P<|A<|C<|V<)'))) {
            if (text.length >= 10 && text.length < 30) text = text.padRight(30, '<');
            else if (text.length > 30 && text.length < 36) text = text.padRight(36, '<');
            else if (text.length > 36 && text.length < 44) text = text.padRight(44, '<');
          } else if (text.contains('<')) {
            if (text.length >= 25 && text.length < 30) text = text.padRight(30, '<');
            else if (text.length >= 31 && text.length < 36) text = text.padRight(36, '<');
            else if (text.length >= 39 && text.length < 44) text = text.padRight(44, '<');
          }
          
          // La ligne doit maintenant faire exactement 30, 36 ou 44 caractères, et si c'est la ligne 1, elle n'a peut-être pas de < à la base mais a été paddée
          bool validLength = text.length == 30 || text.length == 36 || text.length == 44;
          if (validLength && (text.contains('<') || text.startsWith(RegExp(r'^(ID|I<|P<|A<|C<|V<)')))) {
            mrzLines.add(text);
          }
        }
      }

      if (mrzLines.isNotEmpty) {
        int expectedLines = mrzLines[0].length == 30 ? 3 : 2;
        
        if (mrzLines.length >= expectedLines) {
          // On prend exactement le nombre de lignes attendues (au cas où il y a des doublons lus)
          List<String> finalLines = mrzLines.sublist(0, expectedLines);
          
          bool allSameLength = finalLines.every((l) => l.length == finalLines[0].length);
          bool hasLine1 = finalLines[0].startsWith(RegExp(r'^(ID|I<|P<|A<|C<|V<)'));
          
          if (allSameLength && hasLine1) {
            String foundMrz = finalLines.join('\n');
            if (foundMrz != _mrzResult) {
              setState(() {
                _mrzResult = foundMrz;
              });
            }
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
    TextEditingController mrzController = TextEditingController(text: _mrzResult);
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (BuildContext context) {
        return AlertDialog(
          title: const Text("Vérifiez le MRZ"),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text("Corrigez manuellement (ex: 0 au lieu de O) si l'appareil photo s'est trompé. Une seule erreur bloquera le NFC !"),
              const SizedBox(height: 10),
              TextField(
                controller: mrzController,
                maxLines: 3,
                style: const TextStyle(fontFamily: 'monospace'),
                decoration: const InputDecoration(border: OutlineInputBorder()),
              ),
            ],
          ),
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
              child: const Text("Scanner via USB (PC)", style: TextStyle(color: Colors.white)),
              style: ElevatedButton.styleFrom(backgroundColor: Colors.blueGrey),
              onPressed: () async {
                String doc = "";
                String dob = "";
                String exp = "";
                List<String> lines = mrzController.text.split('\n');
                if (lines.length >= 2) {
                   if (lines[0].length >= 30) {
                      doc = lines[0].substring(5, 14).replaceAll('<', '');
                      dob = lines[1].substring(0, 6);
                      exp = lines[1].substring(8, 14);
                   } else if (lines.length >= 2 && lines[1].length >= 44) {
                      doc = lines[1].substring(0, 9).replaceAll('<', '');
                      dob = lines[1].substring(13, 19);
                      exp = lines[1].substring(21, 27);
                   }
                }
                var payload = {
                   "documentNumber": doc,
                   "dateOfBirth": dob,
                   "expiryDate": exp
                };
                
                String targetUrl = widget.uploadUrl.replaceAll('/upload', '/mrz-only');
                Navigator.of(context).pop(); // Fermer la modale immédiatement
                ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("Envoi au PC en cours...")));
                
                try {
                  final response = await http.post(
                    Uri.parse(targetUrl),
                    headers: {"Content-Type": "application/json"},
                    body: jsonEncode({"type": "MRZ_DATA", "data": payload})
                  ).timeout(const Duration(seconds: 5));
                  
                  if(response.statusCode == 200) {
                      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text("✅ MRZ reçue par le PC !"), backgroundColor: Colors.green));
                  } else {
                      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("❌ Erreur HTTP ${response.statusCode}"), backgroundColor: Colors.red));
                  }
                } catch(e) {
                  ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text("❌ Timeout : Vérifiez l'adresse IP ($targetUrl)"), backgroundColor: Colors.red));
                }
              },
            ),
            ElevatedButton(
              child: const Text("Continuer sur Mobile"),
              onPressed: () {
                Navigator.of(context).pop();
                  Navigator.pushReplacement(
                    context,
                    MaterialPageRoute(
                      builder: (context) {
                        return IdFrontScannerScreen(
                          mrzText: _mrzResult, 
                          cameras: widget.cameras, 
                          uploadUrl: widget.uploadUrl
                        );
                      }
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
          
          // Surcouche visuelle (cadre pour guider l'utilisateur)
          if (!_isLocked)
            Positioned.fill(
              child: CustomPaint(
                painter: MrzOverlayPainter(),
              ),
            ),
            
          // Guide textuel au-dessus du cadre
          if (!_isLocked)
            Positioned(
              top: MediaQuery.of(context).size.height * 0.35,
              left: 0,
              right: 0,
              child: const Text(
                "Alignez la bande MRZ dans ce cadre",
                textAlign: TextAlign.center,
                style: TextStyle(
                  color: Colors.greenAccent,
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                  backgroundColor: Colors.black45
                ),
              ),
            ),
            
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


class MrzOverlayPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()..color = Colors.black.withOpacity(0.6);
    final path = Path()..addRect(Rect.fromLTWH(0, 0, size.width, size.height));
    
    // Taille typique de la bande MRZ
    final double mrzHeight = size.height * 0.18;
    final double mrzWidth = size.width * 0.95;
    
    final cutout = Path()
      ..addRRect(RRect.fromRectAndRadius(
        Rect.fromCenter(
          center: Offset(size.width / 2, size.height * 0.5),
          width: mrzWidth,
          height: mrzHeight,
        ),
        const Radius.circular(8),
      ));

    // Combine paths (zone assombrie autour du cadre)
    final background = Path.combine(PathOperation.difference, path, cutout);
    canvas.drawPath(background, paint);

    // Dessiner une bordure pour guider
    final borderPaint = Paint()
      ..color = Colors.greenAccent
      ..style = PaintingStyle.stroke
      ..strokeWidth = 3.0;
    
    // Dessiner des coins (corners)
    final double cornerLength = 30.0;
    final Rect rect = Rect.fromCenter(
      center: Offset(size.width / 2, size.height * 0.5),
      width: mrzWidth,
      height: mrzHeight,
    );
    
    // Top Left
    canvas.drawLine(rect.topLeft, rect.topLeft + Offset(cornerLength, 0), borderPaint);
    canvas.drawLine(rect.topLeft, rect.topLeft + Offset(0, cornerLength), borderPaint);
    
    // Top Right
    canvas.drawLine(rect.topRight, rect.topRight + Offset(-cornerLength, 0), borderPaint);
    canvas.drawLine(rect.topRight, rect.topRight + Offset(0, cornerLength), borderPaint);
    
    // Bottom Left
    canvas.drawLine(rect.bottomLeft, rect.bottomLeft + Offset(cornerLength, 0), borderPaint);
    canvas.drawLine(rect.bottomLeft, rect.bottomLeft + Offset(0, -cornerLength), borderPaint);
    
    // Bottom Right
    canvas.drawLine(rect.bottomRight, rect.bottomRight + Offset(-cornerLength, 0), borderPaint);
    canvas.drawLine(rect.bottomRight, rect.bottomRight + Offset(0, -cornerLength), borderPaint);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
