# -*- coding: utf-8 -*-
"""
Génère des extraits d'acte de naissance FICTIFS pour tester FRIDA.

Chaque page est une image A4 portant un QR code au format lu par l'OCR (entité en_01_qrcode_01) :
séquences séparées par « * », nom et prénom en positions 4 et 5, date en 6, sexe en 12,
NIN = premiers 18 chiffres consécutifs. Le QR code est placé dans la zone que l'OCR
découpe (bas gauche de la page). Toutes les identités sont inventées, les NIN commencent
par 99, et chaque page porte la mention « DOCUMENT DE TEST FICTIF » : ces fichiers ne
reproduisent aucun acte réel.

Avec --arabe, les identités portent des noms et prénoms arabes réalistes (graphie arabe
dans le QR code, translittération latine en positions 13 et 14, filiation cohérente dans
la famille) : utile pour tester la vérification phonétique et l'affichage des noms.

Usage :
    pip install qrcode pillow [arabic-reshaper python-bidi]
    python scripts/generer_extraits_test.py [dossier_sortie] [--fils N] [--filles N] [--arabe]
"""
import argparse
import json
import os

import qrcode
from PIL import Image, ImageDraw, ImageFont

# Zone QR de l'entité en_01_qrcode_01 (coordonnées relatives x1, y1, x2, y2)
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


# Famille à noms arabes : (prénom arabe, prénom latin) pour chaque rôle
FAMILLE = ("بلقاسم", "BELKACEM")
PRENOMS_FILS = [("محمد", "MOHAMED"), ("يوسف", "YOUCEF"), ("كريم", "KARIM"),
                ("عمر", "OMAR"), ("سليم", "SALIM"), ("رضا", "REDHA")]
PRENOMS_FILLES = [("أمينة", "AMINA"), ("سارة", "SARA"), ("نسرين", "NESRINE"),
                  ("ليلى", "LEILA"), ("خديجة", "KHADIDJA"), ("مريم", "MERIEM")]


def identites_arabes(fichier, i=0):
    """(nom, prénom, nom latin, prénom latin, prénom du père, nom de la mère, prénom de la mère, lieu)."""
    parents_defunt = ("عبد القادر", "منصوري", "زهرة", "بجاية")
    table = {
        "defunt": ("بلقاسم", "أحمد", "BELKACEM", "AHMED") + parents_defunt,
        "epouse": ("بوزيد", "فاطمة", "BOUZID", "FATIMA", "علي", "خالدي", "عائشة", "سطيف"),
        "pere": ("بلقاسم", "عبد القادر", "BELKACEM", "ABDELKADER", "محمد", "بن يوسف", "خيرة", "بجاية"),
        "mere": ("منصوري", "زهرة", "MANSOURI", "ZOHRA", "سعيد", "عمراني", "يمينة", "أقبو"),
        "frere": ("بلقاسم", "مصطفى", "BELKACEM", "MUSTAPHA") + parents_defunt,
        "soeur": ("بلقاسم", "حورية", "BELKACEM", "HOURIA") + parents_defunt,
    }
    if fichier in table:
        return table[fichier]
    prenoms = PRENOMS_FILS if fichier.startswith("fils") else PRENOMS_FILLES
    prenom_ar, prenom_lat = prenoms[(i - 1) % len(prenoms)]
    return (FAMILLE[0], prenom_ar, FAMILLE[1], prenom_lat, "أحمد", "بوزيد", "فاطمة", "الجزائر")


def nin_fictif(rang, sexe):
    """18 chiffres, préfixe 99 : ne correspond à aucun NIN réel."""
    return "99%d%015d" % (1 if sexe == MASCULIN else 2, rang)


def contenu_qr(ident, sexe, date, nin, rang):
    """Texte du QR code, découpé par l'OCR sur « * » (voir ocr_engine_v2.py)."""
    nom, prenom, nom_lat, prenom_lat, pere, mere_nom, mere_prenom, lieu = ident
    sequences = [
        "EN", "TEST", "T%05d" % rang, "COMMUNE-TEST",
        nom, prenom, date, "00H00", lieu,              # 4 nom, 5 prénom, 6 date, 8 lieu
        pere, mere_nom, mere_prenom,                   # 9 père, 10-11 mère
        sexe, nom_lat, prenom_lat, "BUREAU-TEST",      # 12 sexe, 13-14 latin, 15 délivré par
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


def texte_arabe(texte):
    """Façonne le texte arabe pour Pillow sans libraqm (lettres liées, sens droite-gauche)."""
    try:
        import arabic_reshaper
        from bidi.algorithm import get_display
        return get_display(arabic_reshaper.reshape(texte))
    except ImportError:
        return None


def generer_page(chemin, role, ident, sexe, date, nin, contenu):
    nom, prenom, nom_lat, prenom_lat = ident[:4]
    page = Image.new("RGB", (LARGEUR, HAUTEUR), "white")
    dessin = ImageDraw.Draw(page)
    dessin.text((80, 80), "EXTRAIT D'ACTE DE NAISSANCE", fill="black", font=police(48))
    dessin.text((80, 150), "DOCUMENT DE TEST FICTIF - SANS VALEUR", fill=(200, 0, 0), font=police(40))
    lignes = [
        "Rôle prévu     : %s" % role,
        "Nom / prénom   : %s %s" % (nom_lat, prenom_lat),
        "Sexe           : %s" % ("masculin" if sexe == MASCULIN else "féminin"),
        "Date naissance : %s" % date,
        "NIN fictif     : %s" % nin,
    ]
    for i, ligne in enumerate(lignes):
        dessin.text((80, 280 + i * 60), ligne, fill="black", font=police(34))
    if nom != nom_lat:
        arabe = texte_arabe("%s %s" % (nom, prenom))
        if arabe:
            dessin.text((LARGEUR - 80, 600), arabe, fill="black", font=police(44), anchor="ra")
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
    parser.add_argument("--arabe", action="store_true", help="noms et prénoms arabes réalistes")
    args = parser.parse_args()
    os.makedirs(args.sortie, exist_ok=True)

    manifeste = []
    for rang, (fichier, role, sexe, prenom, date, code) in enumerate(personnes(args.fils, args.filles), start=1):
        if args.arabe:
            numero = int(fichier.rsplit("_", 1)[1]) if fichier[-1].isdigit() else 0
            ident = identites_arabes(fichier, numero)
        else:
            ident = ("TEST", prenom, "TEST", prenom, "PERE-TEST", "MERE", "TEST", "ALGER-TEST")
        nin = nin_fictif(rang, sexe)
        chemin = os.path.join(args.sortie, fichier + ".png")
        generer_page(chemin, role, ident, sexe, date, nin, contenu_qr(ident, sexe, date, nin, rang))
        manifeste.append({"fichier": fichier + ".png", "role": role, "code": code,
                          "nom": ident[0], "prenom": ident[1], "nomLatin": ident[2], "prenomLatin": ident[3],
                          "sexe": sexe, "dateNaissance": date, "nin": nin})
        print("%-14s %-18s %-22s NIN %s" % (fichier + ".png", role, ident[2] + " " + ident[3], nin))

    with open(os.path.join(args.sortie, "manifeste.json"), "w", encoding="utf-8") as f:
        json.dump(manifeste, f, ensure_ascii=False, indent=2)
    print("\n%d extraits fictifs dans %s" % (len(manifeste), os.path.abspath(args.sortie)))


if __name__ == "__main__":
    main()
