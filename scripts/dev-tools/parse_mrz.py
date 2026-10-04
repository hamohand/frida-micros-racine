
def parse_mrz(mrz_bytes):
    try:
        mrz_str = mrz_bytes.decode('ascii')
        # Typical MRZ Type 3 (Passport): 2 lines of 44 chars
        # Typical MRZ Type 1 (ID Card): 3 lines of 30 chars
        # The names are always in the first line for Type 1, or first line for Passport
        # Format for passport: P<D<<LASTNAME<<FIRSTNAME<<<<<
        lines = []
        if len(mrz_str) >= 88:
            lines = [mrz_str[0:44], mrz_str[44:88]]
        elif len(mrz_str) >= 90:
            lines = [mrz_str[0:30], mrz_str[30:60], mrz_str[60:90]]
            
        if not lines:
            return {'nom': 'Inconnu', 'prenom': 'Inconnu'}
            
        name_line = lines[0]
        if name_line[0] in ('P', 'V'):
            name_part = name_line[5:]
        elif name_line[0] in ('I', 'A', 'C'):
            name_part = lines[0][5:] if len(lines[0]) == 30 else lines[0][5:] # ID card
        else:
            name_part = name_line[5:]
            
        parts = name_part.split('<<')
        nom = parts[0].replace('<', ' ').strip()
        prenom = parts[1].replace('<', ' ').strip() if len(parts) > 1 else ''
        return {'nom': nom, 'prenom': prenom}
    except Exception:
        return {'nom': 'Inconnu', 'prenom': 'Inconnu'}

