import requests
import json

base_url = 'http://localhost:8082'

# 1. Upload dummy image
with open('../easytess_ocr_api/backend/app_ocr/uploads/entities/cni_01/reference.jpg', 'rb') as f:
    files = {'image': f}
    r = requests.post(f'{base_url}/api/upload', files=files)
    print("Upload response:", r.text)
    upload_data = r.json()
    filename = upload_data.get('saved_filename', upload_data.get('filename'))

# 2. Analyze
payload = {
    'filename': filename,
    'mode': 'rapide',
    'zones': {
        'nom': {
            'coords': [0.65, 0.62, 0.91, 0.69],
            'lang': 'ara',
            'type': 'ancre'
        },
        'prenom': {
            'coords': [0.65, 0.72, 0.91, 0.80],
            'lang': 'ara',
            'type': 'ancre'
        }
    }
}
print("Analyzing...")
try:
    r2 = requests.post(f'{base_url}/api/analyser', json=payload, timeout=10)
    print("Analyze response:", r2.text)
except Exception as e:
    print("Error:", e)
