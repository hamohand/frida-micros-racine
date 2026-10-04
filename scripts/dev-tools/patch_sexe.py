
with open('frida-usb-agent/emrtd.py', 'r', encoding='utf-8') as f:
    text = f.read()

new_parse_mrz = '''def _parse_mrz_data(dg1_bytes):
    try:
        idx = dg1_bytes.find(b'\\x5f\\x1f')
        if idx != -1:
            length = dg1_bytes[idx+2]
            mrz_str = dg1_bytes[idx+3:idx+3+length].decode('ascii', errors='ignore').replace('\\n', '').replace('\\r', '')
            
            lines = []
            sexe = 'Inconnu'
            if len(mrz_str) >= 88 and len(mrz_str) < 90:
                lines = [mrz_str[0:44], mrz_str[44:88]]
                name_part = lines[0][5:]
                sexe = lines[1][20]
            elif len(mrz_str) >= 90:
                lines = [mrz_str[0:30], mrz_str[30:60], mrz_str[60:90]]
                name_part = lines[2]
                sexe = lines[1][7]
            else:
                name_part = mrz_str
                
            parts = name_part.split('<<')
            nom = parts[0].replace('<', ' ').strip()
            prenom = parts[1].replace('<', ' ').strip() if len(parts) > 1 else ''
            
            if sexe == 'M': sexe = 'Masculin'
            elif sexe == 'F': sexe = 'Féminin'
            
            return nom, prenom, sexe
    except Exception:
        pass
    return 'Inconnu', 'Inconnu', 'Inconnu'
'''

import re
text = re.sub(r'def _parse_mrz_data.*?return \'Inconnu\', \'Inconnu\'\n', new_parse_mrz, text, flags=re.DOTALL)
text = text.replace('nom, prenom = _parse_mrz_data(dg1)', 'nom, prenom, sexe = _parse_mrz_data(dg1)')

with open('frida-usb-agent/emrtd.py', 'w', encoding='utf-8') as f:
    f.write(text)

