# -*- coding: utf-8 -*-
"""
Génère des extraits d'acte de naissance FICTIFS pour tester FRIDA.

Chaque page est une image A4 portant un QR code au format lu par l'OCR (entité en_01) :
séquences séparées par « * », nom et prénom en positions 4 et 5, date en 6, sexe en 12,
NIN = premiers 18 chiffres consécutifs. Le QR code est placé dans la zone que l'OCR
découpe (bas gauche de la page). Toutes les identités sont inventées, les NIN commencent
par 99, et chaque page porte la mention « DOCUMENT DE TEST FICTIF » : ces fichiers ne
reproduisent aucun acte réel.

Usage :
    pip install qrcode pillow
    python scripts/generer_extraits_test.py [dossier_sortie] [--fils N] [--filles N]
"""
import argparse
import json
import os

import qrcode
from PIL import Image, ImageDraw, ImageFont

# Zone QR de l'entité en_01 (coordonnées relatives x1, y1, x2, y2)
ZONE_QR = (0.0761789600967352, 0.7360319270239453, 0.40870616686819833, 0.9729190421892816)
LARGEUR, HAUTEUR = 1240, 1754  # A4 à 150 dpi
MASCULIN, FEMININ = "ذكر", "أنثى"


def personnes(nb_fils, nb_filles):
    """Famille de test : (fichier, rôle, sexe, prénom, date de naissance, code de parenté)."""
    liste = [
        ("defunt", "Defunt", MASCULIN, "DEFUNT", "1950-01-01", "01"),
        ("epouse", "Conjoint (epouse)", FEMININ, "EPOUSE", "1955-05-05", "02"),
    ]
    liste += [("fils_%d" % i, "Fils %d" % i, MASCULIN, "FILS%d" % i, "%d-03-15" % (1979 + i), "03")
              for i in range(1, nb_fils + 1)]
    liste += [("fille_%d" % i, "Fille %d" % i, FEMININ, "FILLE%d" % i, "%d-07-20" % (1984 + i), "03")
              for i in range(1, nb_filles + 1)]
    liste += [
        ("pere", "Pere", MASCULIN, "PERE", "1920-02-02", "04"),
        ("mere", "Mere", FEMININ, "MERE", "1925-06-06", "04"),
        ("frere", "Frere", MASCULIN, "FRERE", "1952-08-08", "05"),
        ("soeur", "Soeur", FEMININ, "SOEUR", "1953-09-09", "05"),
    ]
    return liste


def nin_fictif(rang, sexe):
    """18 chiffres, préfixe 99 : ne correspond à aucun NIN réel."""
    return "99%d%015d" % (1 if sexe == MASCULIN else 2, rang)


def contenu_qr(prenom, sexe, date, nin, rang):
    """Texte du QR code, découpé par l'OCR sur « * » (voir ocr_engine_v2.py)."""
    sequences = [
        "EN", "TEST", "T%05d" % rang, "COMMUNE-TEST",
        "TEST", prenom, date, "00H00", "ALGER-TEST",   # 4 nom, 5 prénom, 6 date, 8 lieu
        "PERE-TEST", "MERE", "TEST",                   # 9 père, 10-11 mère
        sexe, "TEST", prenom, "BUREAU-TEST",           # 12 sexe, 13-14 latin, 15 délivré par
        nin,
    ]
    # L'OCR n'extrait sexe, date et NIN qu'au-delà de 26 séquences non vides
    sequences += ["X%d" % i for i in range(1, 27 - len(sequences))]
    return "*".join(sequences)


def police(taille):
    for nom in ("arial.ttf", "DejaVuSans.ttf"):
        try:
            return ImageFont.truetype(nom, taille)
        except OSError:
            pass
    return ImageFont.load_default(size=taille)


def generer_page(chemin, role, prenom, sexe, date, nin, contenu):
    page = Image.new("RGB", (LARGEUR, HAUTEUR), "white")
    dessin = ImageDraw.Draw(page)
    dessin.text((80, 80), "EXTRAIT D'ACTE DE NAISSANCE", fill="black", font=police(48))
    dessin.text((80, 150), "DOCUMENT DE TEST FICTIF - SANS VALEUR", fill=(200, 0, 0), font=police(40))
    lignes = [
        "Rôle prévu     : %s" % role,
        "Nom / prénom   : TEST %s" % prenom,
        "Sexe           : %s" % ("masculin" if sexe == MASCULIN else "féminin"),
        "Date naissance : %s" % date,
        "NIN fictif     : %s" % nin,
    ]
    for i, ligne in enumerate(lignes):
        dessin.text((80, 280 + i * 60), ligne, fill="black", font=police(34))
    dessin.text((80, 700), "SPECIMEN", fill=(235, 235, 235), font=police(220))

    # QR code centré dans la zone lue par l'OCR, sans interpolation des modules
    qr = qrcode.QRCode(error_correction=qrcode.constants.ERROR_CORRECT_M, box_size=1, border=4)
    qr.add_data(contenu.encode("utf-8"))
    qr.make(fit=True)
    modules = qr.modules_count + 2 * qr.border
    x1, y1 = int(ZONE_QR[0] * LARGEUR), int(ZONE_QR[1] * HAUTEUR)
    x2, y2 = int(ZONE_QR[2] * LARGEUR), int(ZONE_QR[3] * HAUTEUR)
    pixels_par_module = max(1, int(min(x2 - x1, y2 - y1) * 0.9) // modules)
    image_qr = qr.make_image(fill_color="black", back_color="white").convert("RGB")
    cote = modules * pixels_par_module
    image_qr = image_qr.resize((cote, cote), Image.NEAREST)
    page.paste(image_qr, (x1 + (x2 - x1 - cote) // 2, y1 + (y2 - y1 - cote) // 2))
    page.save(chemin)


def main():
    parser = argparse.ArgumentParser(description="Génère des extraits de naissance fictifs pour tester FRIDA.")
    parser.add_argument("sortie", nargs="?", default="extraits-test")
    parser.add_argument("--fils", type=int, default=3)
    parser.add_argument("--filles", type=int, default=2)
    args = parser.parse_args()
    os.makedirs(args.sortie, exist_ok=True)

    manifeste = []
    for rang, (fichier, role, sexe, prenom, date, code) in enumerate(personnes(args.fils, args.filles), start=1):
        nin = nin_fictif(rang, sexe)
        chemin = os.path.join(args.sortie, fichier + ".png")
        generer_page(chemin, role, prenom, sexe, date, nin, contenu_qr(prenom, sexe, date, nin, rang))
        manifeste.append({"fichier": fichier + ".png", "role": role, "code": code, "nom": "TEST",
                          "prenom": prenom, "sexe": sexe, "dateNaissance": date, "nin": nin})
        print("%-14s %-18s NIN %s" % (fichier + ".png", role, nin))

    with open(os.path.join(args.sortie, "manifeste.json"), "w", encoding="utf-8") as f:
        json.dump(manifeste, f, ensure_ascii=False, indent=2)
    print("\n%d extraits fictifs dans %s" % (len(manifeste), os.path.abspath(args.sortie)))


if __name__ == "__main__":
    main()
