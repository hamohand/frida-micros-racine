import os

file_path = '../easytess_ocr_api/backend/app_ocr/app/services/ocr_engine.py'
with open(file_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()

in_func = False
out_lines = []
for i, line in enumerate(lines):
    if line.strip() == '"""' and 'Returns:' in lines[i-2]:
        in_func = True
        out_lines.append(line)
        continue
        
    if in_func:
        if line.startswith('def get_absolute_coords('):
            in_func = False
            out_lines.append(line)
            continue
            
        if line.startswith('    ') and line.strip() != '':
            out_lines.append(line[4:])
        elif line.strip() == '':
            out_lines.append('\n')
        else:
            out_lines.append(line)
    else:
        out_lines.append(line)

with open(file_path, 'w', encoding='utf-8') as f:
    f.writelines(out_lines)
print("Done")
