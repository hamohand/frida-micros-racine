"""
Génère deux actes de décès de test (père et mère de la famille Kacem)
au format PNG avec QR code embarqué, compatibles avec l'entité adc_01.

Format QR code (séparateur *, avec vides à positions 6, 13, 15, 18
pour correspondre aux indices filtrés de adc_01.json) :
  raw[2]  → numeroPiece    filtered[2]
  raw[3]  → nom            filtered[3]
  raw[4]  → prenom         filtered[4]
  raw[5]  → dateNaissance  filtered[5]
  raw[7]  → lieuNaissance  filtered[6]  (raw[6] vide)
  raw[10] → latines        filtered[9]
  raw[11] → prenomLatines  filtered[10]
  raw[14] → pere           filtered[12] (raw[13] vide)
  raw[16] → mere_prenom    filtered[13] (raw[15] vide)
  raw[17] → mere_nom       filtered[14]
  raw[19] → dateDeces      filtered[15] (raw[18] vide)
  raw[20] → heureDeces     filtered[16]
  raw[21] → lieuDeces      filtered[17]
"""

import qrcode
import arabic_reshaper
from bidi.algorithm import get_display
from PIL import Image, ImageDraw, ImageFont
import os

import sys

FONT_LATIN = "C:/Windows/Fonts/arialbd.ttf"
FONT_LATIN_REG = "C:/Windows/Fonts/arial.ttf"
FONT_ARABIC = "C:/Windows/Fonts/tahoma.ttf"
OUT_DIR = sys.argv[1] if len(sys.argv) > 1 else os.path.dirname(os.path.abspath(__file__))
os.makedirs(OUT_DIR, exist_ok=True)

W, H = 1240, 1754  # A4 @ 150 dpi


def arabic(text):
    return get_display(arabic_reshaper.reshape(text))


def make_qr(data: str) -> Image.Image:
    qr = qrcode.QRCode(version=None, error_correction=qrcode.constants.ERROR_CORRECT_M, box_size=6, border=2)
    qr.add_data(data)
    qr.make(fit=True)
    return qr.make_image(fill_color="black", back_color="white").convert("RGB")


def contains_arabic(text):
    return any('؀' <= c <= 'ۿ' for c in text)


def draw_field(draw, label, value, y, font_label, font_value, font_arabic, color_value=(0, 0, 0)):
    draw.text((60, y), label, font=font_label, fill=(80, 80, 80))
    if contains_arabic(value):
        display_val = arabic(value)
        draw.text((320, y), display_val, font=font_arabic, fill=color_value)
    else:
        draw.text((320, y), value, font=font_value, fill=color_value)
    return y + 60


def generer(nom_fichier, qr_data, titre_role, nom_latin, prenom_latin,
            nom_arabe, prenom_arabe, date_naissance, date_deces, lieu_deces,
            pere, mere_prenom, mere_nom):

    img = Image.new("RGB", (W, H), "white")
    draw = ImageDraw.Draw(img)

    f_titre = ImageFont.truetype(FONT_LATIN, 38)
    f_alerte = ImageFont.truetype(FONT_LATIN, 26)
    f_label = ImageFont.truetype(FONT_LATIN_REG, 28)
    f_value = ImageFont.truetype(FONT_LATIN, 28)
    f_arabe = ImageFont.truetype(FONT_ARABIC, 36)
    f_specimen = ImageFont.truetype(FONT_LATIN, 130)

    # Titre
    draw.text((60, 50), "ACTE DE DÉCÈS", font=f_titre, fill=(0, 0, 0))
    draw.text((60, 100), "DOCUMENT DE TEST FICTIF - SANS VALEUR", font=f_alerte, fill=(200, 0, 0))

    # Ligne séparatrice
    draw.line([(60, 148), (W - 60, 148)], fill=(180, 180, 180), width=2)

    # Champs
    y = 175
    y = draw_field(draw, "Rôle prévu   :", titre_role, y, f_label, f_value, f_arabe)
    y = draw_field(draw, "Nom / prénom :", f"{nom_latin} {prenom_latin}", y, f_label, f_value, f_arabe)
    y = draw_field(draw, "Date naissance :", date_naissance, y, f_label, f_value, f_arabe)
    y = draw_field(draw, "Date de décès :", date_deces, y, f_label, f_value, f_arabe)
    y = draw_field(draw, "Lieu de décès :", lieu_deces, y, f_label, f_value, f_arabe)
    y = draw_field(draw, "Père          :", pere, y, f_label, f_value, f_arabe)
    y = draw_field(draw, "Mère          :", f"{mere_prenom} {mere_nom}", y, f_label, f_value, f_arabe)

    # Pas de NIN — acte de décès

    # Nom arabe (aligné à droite)
    arabe_str = arabic(f"{prenom_arabe} {nom_arabe}")
    bbox = draw.textbbox((0, 0), arabe_str, font=f_arabe)
    tw = bbox[2] - bbox[0]
    draw.text((W - 60 - tw, y + 10), arabe_str, font=f_arabe, fill=(0, 0, 80))

    # Watermark SPECIMEN
    spec_img = Image.new("RGBA", (W, H), (255, 255, 255, 0))
    spec_draw = ImageDraw.Draw(spec_img)
    bbox_s = spec_draw.textbbox((0, 0), "SPECIMEN", font=f_specimen)
    sw, sh = bbox_s[2] - bbox_s[0], bbox_s[3] - bbox_s[1]
    spec_draw.text(((W - sw) // 2, (H - sh) // 2), "SPECIMEN", font=f_specimen, fill=(200, 200, 200, 90))
    img = Image.alpha_composite(img.convert("RGBA"), spec_img).convert("RGB")
    draw = ImageDraw.Draw(img)

    # QR code
    qr_img = make_qr(qr_data)
    qr_size = 320
    qr_img = qr_img.resize((qr_size, qr_size), Image.LANCZOS)
    img.paste(qr_img, (60, H - qr_size - 60))

    out_path = os.path.join(OUT_DIR, nom_fichier)
    img.save(out_path, "PNG")
    print(f"Généré : {out_path}")


# ─── Père : ABDELKADER KACEM (code 04, né 1920-02-02) ───────────────────
#
# Format QR : AD*TEST*T04001*nom*prenom*dateNaiss**lieuNaiss*X8*X9*latines*prenomLatines*X11**pere**merePrenom*mereNom**dateDeces*heureDeces*lieuDeces
# Positions raw : 0  1     2       3        4        5      6(vide) 7    8   9   10       11       12   13(vide) 14 15(vide) 16          17      18(vide) 19           20          21

QR_PERE = (
    "AD*TEST*T04001*قاسم*عبد القادر*02/02/1920**بجاية*X8*X9"
    "*KACEM*ABDELKADER*X11**محمد**زهرة*بوديف**15/06/1985*10:00*بجاية"
)

generer(
    nom_fichier="deces_pere.png",
    qr_data=QR_PERE,
    titre_role="Pere (acte de décès)",
    nom_latin="KACEM",
    prenom_latin="ABDELKADER",
    nom_arabe="قاسم",
    prenom_arabe="عبد القادر",
    date_naissance="02/02/1920",
    date_deces="15/06/1985",
    lieu_deces="بجاية",
    pere="محمد",
    mere_prenom="زهرة",
    mere_nom="بوديف",
)

# ─── Mère : ZAHRA NASOURI (code 04, née 1925-06-06) ───────────────────────
QR_MERE = (
    "AD*TEST*T04002*نصوري*زهرة*06/06/1925**الجزائر*X8*X9"
    "*NASOURI*ZAHRA*X11**عمر**مريم*حمزة**20/11/1990*14:00*العلمة"
)

generer(
    nom_fichier="deces_mere.png",
    qr_data=QR_MERE,
    titre_role="Mere (acte de décès)",
    nom_latin="NASOURI",
    prenom_latin="ZAHRA",
    nom_arabe="منصوري",
    prenom_arabe="زهرة",
    date_naissance="06/06/1925",
    date_deces="20/11/1990",
    lieu_deces="العلمة",
    pere="عمر",
    mere_prenom="مريم",
    mere_nom="حمزة",
)

# ─── Défunt : AHMED KACEM (code 01, né 01/01/1950) ─────────────────────────
QR_DEFUNT = (
    "AD*TEST*T01001*قاسم*أحمد*01/01/1950**بجاية*X8*X9"
    "*KACEM*AHMED*X11**عبد القادر**زهرة*منصوري**10/01/2025*09:30*بجاية"
)

generer(
    nom_fichier="deces_defunt.png",
    qr_data=QR_DEFUNT,
    titre_role="Defunt (acte de décès)",
    nom_latin="KACEM",
    prenom_latin="AHMED",
    nom_arabe="قاسم",
    prenom_arabe="أحمد",
    date_naissance="01/01/1950",
    date_deces="10/01/2025",
    lieu_deces="بجاية",
    pere="عبد القادر",
    mere_prenom="زهرة",
    mere_nom="منصوري",
)

