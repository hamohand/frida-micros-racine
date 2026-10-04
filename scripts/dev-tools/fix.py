
with open('frontend/src/app/components/dossier/nfc-scanner-modal/nfc-scanner-modal.component.ts', 'r', encoding='utf-8') as f:
    lines = f.readlines()

for i, line in enumerate(lines):
    if line.strip() == '\\\]':
        lines[i] = '  ]\n'
        break

with open('frontend/src/app/components/dossier/nfc-scanner-modal/nfc-scanner-modal.component.ts', 'w', encoding='utf-8') as f:
    f.writelines(lines)

