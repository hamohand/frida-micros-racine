import os

file_path = '../easytess_ocr_api/backend/app_ocr/app/services/ocr_engine.py'
with open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

out_lines = []
in_hybride = False
for i, line in enumerate(lines):
    if line.startswith('_analyser_lock = threading.Lock()'):
        out_lines.append('_easyocr_lock = threading.Lock()\n')
        continue
    
    if line.startswith('def analyser_hybride('):
        in_hybride = True
        out_lines.append(line)
        continue
        
    if in_hybride:
        if line.startswith('    with _analyser_lock:'):
            continue
        elif line.startswith('def get_absolute_coords('):
            in_hybride = False
            out_lines.append(line)
            continue
        
        # Check if the line is part of the docstring or definition
        if '"""' in line and not line.strip() == '"""':
            out_lines.append(line)
            continue
        if line.strip() == '"""':
            out_lines.append(line)
            continue
        if line.startswith('    Args:') or line.startswith('        ') or line.startswith('    Returns:'):
            out_lines.append(line)
            continue
            
        # Unindent 4 spaces
        if line.startswith('    '):
            out_lines.append(line[4:])
        else:
            out_lines.append(line)
    else:
        # Check for analyser_avec_easyocr to add the lock
        if line.startswith('                results = reader.readtext(zone_img)'):
            out_lines.append('                with _easyocr_lock:\n')
            out_lines.append('                    results = reader.readtext(zone_img)\n')
        else:
            out_lines.append(line)

with open(file_path, 'w', encoding='utf-8') as f:
    f.writelines(out_lines)
print("done")
