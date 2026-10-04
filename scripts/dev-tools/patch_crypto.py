
with open('frida-usb-agent/emrtd.py', 'r', encoding='utf-8') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if 'enc_data = resp[idx:idx+length-1]' in line:
        lines[i] = '                enc_data = resp[idx+1:idx+length]\n'

with open('frida-usb-agent/emrtd.py', 'w', encoding='utf-8') as f:
    f.writelines(lines)

