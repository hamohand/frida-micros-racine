import re

with open('frontend/src/app/components/frida/frida.component.html', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace class="vert" with class="ocr-data"
content = content.replace('class="vert"', 'class="ocr-data"')

# Wrap fields with <span class="ocr-data">...</span>
fields_to_wrap = [
    '{{frida?.defunt.identite.pere}}',
    '{{frida?.defunt.identite.mere}}',
    '{{frida?.defunt.identite.dateNaissance}}',
    '{{frida?.defunt.identite.lieuNaissance}}',
    '{{frida?.defunt.identite.wilaya}}',
    '{{frida?.defunt.identite.numeroPiece}}',
    '{{temoin.identite.dateNaissance}}',
    '{{temoin.adresse}}',
    '{{heritier.identite.dateNaissance}}',
    '{{heritier.identite.lieuNaissance}}',
    '{{heritier.identite.wilaya}}'
]

for field in fields_to_wrap:
    # Only replace if not already wrapped
    if f'<span class="ocr-data">{field}</span>' not in content:
        content = content.replace(field, f'<span class="ocr-data">{field}</span>')

with open('frontend/src/app/components/frida/frida.component.html', 'w', encoding='utf-8') as f:
    f.write(content)

print("done")
