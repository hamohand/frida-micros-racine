import hashlib
import os
import struct
from Crypto.Cipher import DES3, DES
from smartcard.System import readers
from smartcard.util import toBytes, toHexString

class BACError(Exception):
    pass

def _pad(data):
    data = bytearray(data)
    data.append(0x80)
    while len(data) % 8 != 0:
        data.append(0x00)
    return bytes(data)

def _adjust_parity(data):
    def parity(b):
        b ^= b >> 4
        b ^= b >> 2
        b ^= b >> 1
        return b & 1

    res = bytearray()
    for b in data:
        b &= 0xFE
        if parity(b) == 0:
            b |= 1
        res.append(b)
    return bytes(res)

def _derive_key(kseed, c):
    h = hashlib.sha1(kseed + c).digest()
    ka = h[:8]
    kb = h[8:16]
    return _adjust_parity(ka + kb)

def _retail_mac(key, data):
    data = _pad(data)
    cipher1 = DES.new(key[:8], DES.MODE_CBC, b'\x00'*8)
    mac = cipher1.encrypt(data)[-8:]
    cipher2 = DES.new(key[8:16], DES.MODE_ECB)
    mac = cipher2.decrypt(mac)
    cipher3 = DES.new(key[:8], DES.MODE_ECB)
    mac = cipher3.encrypt(mac)
    return mac

def _check_digit(s):
    weights = [7, 3, 1]
    tot = 0
    for i, c in enumerate(s):
        if c == '<': v = 0
        elif '0' <= c <= '9': v = int(c)
        elif 'A' <= c <= 'Z': v = ord(c) - 55
        else: v = 0
        tot += v * weights[i % 3]
    return str(tot % 10)

def _get_mrz_info(doc, dob, doe):
    doc = doc.upper().replace(' ', '<')[:9].ljust(9, '<')
    cd_doc = _check_digit(doc)
    dob = dob.upper().replace(' ', '<')[:6].ljust(6, '<')
    cd_dob = _check_digit(dob)
    doe = doe.upper().replace(' ', '<')[:6].ljust(6, '<')
    cd_doe = _check_digit(doe)
    mrz_info = doc + cd_doc + dob + cd_dob + doe + cd_doe
    print(f"\n---> DEBUG MRZ: doc='{doc}' cd='{cd_doc}' dob='{dob}' cd='{cd_dob}' doe='{doe}' cd='{cd_doe}'")
    print(f"---> CHAINE HASHEE: {mrz_info}\n")
    return mrz_info

class eMRTD:
    def __init__(self, connection):
        self.connection = connection
        self.ks_enc = None
        self.ks_mac = None
        self.ssc = None

    def transmit(self, apdu):
        response, sw1, sw2 = self.connection.transmit(apdu)
        return bytes(response), sw1, sw2

    def transmit_sm(self, cla, ins, p1, p2, data=b'', le=None):
        if not self.ks_enc:
            raise BACError("Session non établie (SM)")

        # Increment SSC
        ssc_int = int.from_bytes(self.ssc, 'big') + 1
        self.ssc = ssc_int.to_bytes(8, 'big')

        cmd_header = bytes([cla | 0x0C, ins, p1, p2])
        padded_header = _pad(cmd_header)

        do87 = b''
        if data:
            padded_data = _pad(data)
            cipher = DES3.new(self.ks_enc + self.ks_enc[:8], DES3.MODE_CBC, b'\x00'*8)
            enc_data = cipher.encrypt(padded_data)
            if len(enc_data) > 127:
                do87 = b'\x87\x81' + bytes([len(enc_data) + 1]) + b'\x01' + enc_data
            else:
                do87 = b'\x87' + bytes([len(enc_data) + 1]) + b'\x01' + enc_data

        do97 = b''
        if le is not None:
            do97 = b'\x97\x01' + bytes([le])

        M = self.ssc + padded_header + do87 + do97
        mac = _retail_mac(self.ks_mac, M)
        do8e = b'\x8E\x08' + mac

        final_data = do87 + do97 + do8e
        apdu = list(cmd_header) + [len(final_data)] + list(final_data)
        if le is not None:
            apdu.append(0x00)

        resp, sw1, sw2 = self.transmit(apdu)
        
        # Increment SSC for response
        ssc_int = int.from_bytes(self.ssc, 'big') + 1
        self.ssc = ssc_int.to_bytes(8, 'big')

        # Check DO99 (status)
        # Parse BER-TLV response
        idx = 0
        dec_data = b''
        resp_do87 = b''
        resp_do99 = b''
        
        while idx < len(resp):
            tag = resp[idx]
            if tag == 0x87:
                idx += 1
                length = resp[idx]
                if length == 0x81:
                    idx += 1
                    length = resp[idx]
                elif length == 0x82:
                    idx += 1
                    length = (resp[idx] << 8) + resp[idx+1]
                    idx += 1
                idx += 1
                resp_do87 = b'\x87' + resp[idx-1:idx-1 + length + (1 if resp[idx-2]==0x81 else 2 if resp[idx-3]==0x82 else 1)] # raw
                
                # skip padding indicator (0x01)
                enc_data = resp[idx+1:idx+length]
                idx += length
                cipher = DES3.new(self.ks_enc + self.ks_enc[:8], DES3.MODE_CBC, b'\x00'*8)
                dec_data = cipher.decrypt(enc_data)
                # unpad
                while dec_data and dec_data[-1] == 0x00:
                    dec_data = dec_data[:-1]
                if dec_data and dec_data[-1] == 0x80:
                    dec_data = dec_data[:-1]
            elif tag == 0x99:
                idx += 1
                length = resp[idx]
                idx += 1
                resp_do99 = b'\x99' + bytes([length]) + resp[idx:idx+length]
                idx += length
            elif tag == 0x8E:
                idx += 1
                length = resp[idx]
                idx += 1
                resp_mac = resp[idx:idx+length]
                idx += length
            else:
                break
                
        if sw1 != 0x90 or sw2 != 0x00:
            return dec_data, sw1, sw2
            
        return dec_data, sw1, sw2

    def do_bac(self, doc_num, dob, doe):
        mrz_info = _get_mrz_info(doc_num, dob, doe)
        kseed = hashlib.sha1(mrz_info.encode('ascii')).digest()[:16]
        kenc = _derive_key(kseed, b'\x00\x00\x00\x01')
        kmac = _derive_key(kseed, b'\x00\x00\x00\x02')

        # Select LDS
        _, sw1, sw2 = self.transmit([0x00, 0xA4, 0x04, 0x0C, 0x07, 0xA0, 0x00, 0x00, 0x02, 0x47, 0x10, 0x01])
        if sw1 != 0x90:
            raise BACError("App LDS non trouvée")

        # Get Challenge
        rnd_icc, sw1, sw2 = self.transmit([0x00, 0x84, 0x00, 0x00, 0x08])
        if sw1 != 0x90:
            raise BACError("Get Challenge échoué")

        rnd_ifd = os.urandom(8)
        k_ifd = os.urandom(16)
        S = rnd_ifd + rnd_icc + k_ifd

        cipher = DES3.new(kenc + kenc[:8], DES3.MODE_CBC, b'\x00'*8)
        E_ifd = cipher.encrypt(S)
        M_ifd = _retail_mac(kmac, E_ifd)

        cmd_data = list(E_ifd + M_ifd)
        apdu = [0x00, 0x82, 0x00, 0x00, 0x28] + cmd_data + [0x28]

        resp, sw1, sw2 = self.transmit(apdu)
        if sw1 != 0x90:
            raise BACError("Authentification BAC refusée (vérifiez la MRZ)")

        E_icc = resp[:32]
        M_icc = resp[32:40]

        mac_check = _retail_mac(kmac, E_icc)
        if mac_check != M_icc:
            raise BACError("MAC ICC invalide")

        cipher_dec = DES3.new(kenc + kenc[:8], DES3.MODE_CBC, b'\x00'*8)
        S_icc = cipher_dec.decrypt(E_icc)

        rnd_icc2 = S_icc[:8]
        rnd_ifd2 = S_icc[8:16]
        k_icc = S_icc[16:32]

        if rnd_ifd2 != rnd_ifd:
            raise BACError("RND IFD mismatch")

        kseed_session = bytes(a ^ b for a, b in zip(k_ifd, k_icc))
        self.ks_enc = _derive_key(kseed_session, b'\x00\x00\x00\x01')
        self.ks_mac = _derive_key(kseed_session, b'\x00\x00\x00\x02')
        self.ssc = rnd_icc[-4:] + rnd_ifd[-4:]

    def read_file(self, fid):
        # Select EF
        p1, p2 = fid[0], fid[1]
        _, sw1, sw2 = self.transmit_sm(0x00, 0xA4, 0x02, 0x0C, bytes([p1, p2]))
        
        # Read Binary (first 4 bytes to get length)
        data, sw1, sw2 = self.transmit_sm(0x00, 0xB0, 0x00, 0x00, le=4)
        if not data: return b''
        
        # Parse TLV length
        if data[1] == 0x81:
            length = data[2]
            offset = 3
        elif data[1] == 0x82:
            length = (data[2] << 8) + data[3]
            offset = 4
        else:
            length = data[1]
            offset = 2
            
        total_length = offset + length
        full_data = data
        
        current_offset = 4
        while current_offset < total_length:
            rem = total_length - current_offset
            chunk_size = min(rem, 0xE0) # max read size per APDU
            p1 = (current_offset >> 8) & 0xFF
            p2 = current_offset & 0xFF
            chunk, _, _ = self.transmit_sm(0x00, 0xB0, p1, p2, le=chunk_size)
            if not chunk: break
            full_data += chunk
            current_offset += len(chunk)
            
        return full_data

def _parse_dg11(dg11_bytes):
    data = {}
    idx = 0
    # Skip outer tag 6B
    if idx < len(dg11_bytes) and dg11_bytes[idx] == 0x6B:
        idx += 1
        # skip length
        if dg11_bytes[idx] == 0x81: idx += 2
        elif dg11_bytes[idx] == 0x82: idx += 3
        else: idx += 1

    while idx < len(dg11_bytes):
        if dg11_bytes[idx] in [0x5F, 0x7F, 0xA0]:
            tag = bytes(dg11_bytes[idx:idx+2])
            idx += 2
        else:
            tag = bytes(dg11_bytes[idx:idx+1])
            idx += 1
            
        if idx >= len(dg11_bytes): break
        length = dg11_bytes[idx]
        idx += 1
        
        # A0 is constructed, we enter it
        if tag == b'\xA0':
            continue
            
        val = dg11_bytes[idx:idx+length]
        idx += length
        
        data[tag] = val
        
    def _get_arabic(val):
        parts = val.split(b'<<')
        if len(parts) > 1 and len(parts[-1]) > 0:
            arabic_bytes = parts[-1].rstrip(b'<')
            if len(arabic_bytes) > 0:
                return arabic_bytes.decode('iso-8859-6', errors='ignore')
        return ""

    return {
        'nomArabe': _get_arabic(data.get(b'\x5F\x0E', b'')),
        'prenomArabe': _get_arabic(data.get(b'\x5F\x0F', b'')),
        'lieuNaissanceArabe': _get_arabic(data.get(b'\x5F\x11', b''))
    }

def _parse_mrz_data(dg1_bytes):
    try:
        # Find 5F1F tag
        idx = dg1_bytes.find(b'\x5f\x1f')
        if idx != -1:
            length = dg1_bytes[idx+2]
            mrz_bytes = dg1_bytes[idx+3:idx+3+length]
            mrz_str = mrz_bytes.decode('ascii', errors='ignore').replace('\n', '').replace('\r', '')
            
            lines = []
            sexe = "Inconnu"
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

def read_passport(doc_num, dob, doe):
    try:
        r = readers()
        if not r:
            return {"error": "Aucun lecteur détecté"}
        connection = r[0].createConnection()
        connection.connect()
        
        passport = eMRTD(connection)
        passport.do_bac(doc_num, dob, doe)
        
        # Read DG1 (MRZ text)
        dg1 = passport.read_file([0x01, 0x01])
        nom, prenom, sexe = _parse_mrz_data(dg1)
        
        # Read DG11 (Additional Personal Details)
        dg11 = b''
        nin = ""
        nomArabe = ""
        prenomArabe = ""
        lieuNaissanceArabe = ""
        try:
            dg11 = passport.read_file([0x01, 0x0B])
            import re
            match = re.search(b'\d{18}', dg11)
            if match:
                nin = match.group(0).decode('ascii')
            
            parsed = _parse_dg11(dg11)
            nomArabe = parsed.get('nomArabe', '')
            prenomArabe = parsed.get('prenomArabe', '')
            lieuNaissanceArabe = parsed.get('lieuNaissanceArabe', '')
        except Exception:
            pass
            
        # Read DG12 (Additional Document Details)
        dg12 = b''
        try:
            dg12 = passport.read_file([0x01, 0x0C])
        except Exception:
            pass
            
        return {
            "success": True,
            "message": "Puce lue avec succès",
            "nom": nom,
            "prenom": prenom,
            "sexe": sexe,
            "nomArabe": nomArabe,
            "prenomArabe": prenomArabe,
            "lieuNaissanceArabe": lieuNaissanceArabe,
            "nin": nin,
            "dg1_hex": dg1.hex(),
            "dg11_hex": dg11.hex() if dg11 else "",
            "dg12_hex": dg12.hex() if dg12 else "",
            "documentNumber": doc_num
        }
    except Exception as e:
        return {"error": str(e)}
