import 'package:camera/camera.dart';
import 'package:flutter/material.dart';
import 'mrz_scanner_screen.dart';

List<CameraDescription> cameras = [];

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  try {
    cameras = await availableCameras();
  } catch (e) {
    print('Erreur lors de la récupération des caméras : $e');
  }
  runApp(const FridaMobileApp());
}

class FridaMobileApp extends StatelessWidget {
  const FridaMobileApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Frida Mobile NFC',
      theme: ThemeData(
        primarySwatch: Colors.teal,
        visualDensity: VisualDensity.adaptivePlatformDensity,
      ),
      home: cameras.isEmpty 
          ? const Scaffold(body: Center(child: Text("Aucune caméra trouvée")))
          : MrzScannerScreen(cameras: cameras),
    );
  }
}
