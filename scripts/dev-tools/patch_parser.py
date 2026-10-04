
with open('frida-usb-agent/emrtd.py', 'r', encoding='utf-8') as f:
    text = f.read()

new_parser = '''def _parse_mrz_data(dg1_bytes):
    try:
        idx = dg1_bytes.find(b'\\x5f\\x1f')
        if idx != -1:
            length = dg1_bytes[idx+2]
            mrz_str = dg1_bytes[idx+3:idx+3+length].decode('ascii', errors='ignore').replace('\\n', '').replace('\\r', '')
            
            lines = []
            if len(mrz_str) >= 88 and len(mrz_str) < 90:
                lines = [mrz_str[0:44], mrz_str[44:88]]
                name_line = lines[0]
                name_part = name_line[5:]
            elif len(mrz_str) >= 90:
                lines = [mrz_str[0:30], mrz_str[30:60], mrz_str[60:90]]
                name_line = lines[2]
                name_part = name_line
            else:
                name_part = mrz_str
                
            parts = name_part.split('<<')
            nom = parts[0].replace('<', ' ').strip()
            prenom = parts[1].replace('<', ' ').strip() if len(parts) > 1 else ''
            return nom, prenom
    except Exception:
        pass
    return 'Inconnu', 'Inconnu'
'''

import re
text = re.sub(r'def _parse_mrz_data.*?return \'Inconnu\', \'Inconnu\'\n', new_parser, text, flags=re.DOTALL)

with open('frida-usb-agent/emrtd.py', 'w', encoding='utf-8') as f:
    f.write(text)

