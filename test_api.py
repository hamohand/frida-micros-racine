import urllib.request, json
req = urllib.request.Request('http://localhost:8082/api/analyser', data=json.dumps({'filename':'test.jpg', 'mode':'approfondi', 'zones':{}}).encode('utf-8'), headers={'Content-Type': 'application/json'})
try:
    print(urllib.request.urlopen(req).read().decode('utf-8'))
except Exception as e:
    print(e.read().decode('utf-8'))
