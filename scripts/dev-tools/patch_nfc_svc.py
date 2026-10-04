
with open('frontend/src/app/services/nfc.service.ts', 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('eventSource.addEventListener(\\'NFC_DATA\\', (event: MessageEvent) => {', 'eventSource.addEventListener(\\'MRZ_DATA\\', (event: MessageEvent) => {\\n        try {\\n          const mrzData = JSON.parse(event.data);\\n          subscriber.next({ type: \\'MRZ_DATA\\', data: mrzData } as any);\\n        } catch(e) {}\\n      });\\n\\n      eventSource.addEventListener(\\'NFC_DATA\\', (event: MessageEvent) => {')

with open('frontend/src/app/services/nfc.service.ts', 'w', encoding='utf-8') as f:
    f.write(text)

