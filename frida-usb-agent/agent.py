import sys
import logging
from flask import Flask, request, jsonify
from flask_cors import CORS

try:
    import smartcard.System
    from smartcard.scard import SCardGetErrorMessage
    PCSC_AVAILABLE = True
except ImportError:
    PCSC_AVAILABLE = False
    print("Warning: pyscard n'est pas installé. Lancez: pip install pyscard")

app = Flask(__name__)
CORS(app)

logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

@app.route('/status', methods=['GET'])
def get_status():
    if not PCSC_AVAILABLE:
        return jsonify({"status": "error", "message": "pyscard non installé"}), 500
        
    try:
        readers = smartcard.System.readers()
        return jsonify({
            "status": "ok", 
            "readers": [str(r) for r in readers],
            "message": "Agent Local Actif"
        })
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route('/read', methods=['POST'])
def read_card():
    data = request.json
    doc = data.get('documentNumber')
    dob = data.get('dateOfBirth')
    doe = data.get('dateOfExpiry')
    
    if not doc or not dob or not doe:
        return jsonify({"error": "Données MRZ manquantes"}), 400
        
    logging.info(f"Demande de lecture NFC reçue avec MRZ: {doc} / {dob} / {doe}")
    
    if not PCSC_AVAILABLE:
        return jsonify({"error": "Le module smartcard n'est pas installé"}), 500
        
    try:
        readers = smartcard.System.readers()
        if not readers:
            return jsonify({"error": "Aucun lecteur USB détecté"}), 404
            
        reader = readers[0]
        logging.info(f"Utilisation du lecteur: {reader}")
        
        # ---------------------------------------------------------
        # Implémentation réelle de la lecture eMRTD (Passeport/CNI)
        # ---------------------------------------------------------
        import emrtd
        result = emrtd.read_passport(doc, dob, doe)
        
        if "error" in result:
            return jsonify({"error": result["error"]}), 500
            
        return jsonify(result)
        
    except Exception as e:
        logging.error(f"Erreur lors de la lecture: {e}")
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    print("===================================================")
    print("   🚀 FRIDA USB AGENT (Lecteur NFC uTrust) 🚀   ")
    print("===================================================")
    print(" L'agent écoute sur le port 5000.")
    print(" Assurez-vous que l'interface Web est ouverte.")
    print("===================================================")
    app.run(port=5000, host='0.0.0.0')
