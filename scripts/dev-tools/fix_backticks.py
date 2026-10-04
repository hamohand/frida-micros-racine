
with open('frontend/src/app/components/dossier/nfc-scanner-modal/nfc-scanner-modal.component.ts', 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('  \\]\\n})', '  ]\\n})')

with open('frontend/src/app/components/dossier/nfc-scanner-modal/nfc-scanner-modal.component.ts', 'w', encoding='utf-8') as f:
    f.write(text)

