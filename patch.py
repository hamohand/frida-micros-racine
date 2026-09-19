import json
import sys

engine_path = '../easytess_ocr_api/backend/app_ocr/app/services/ocr_engine.py'
with open(engine_path, 'r', encoding='utf-8') as f:
    content = f.read()

# On va injecter la logique juste après # 1. Détection QR codes
injection_point = "# 2. Zones OCR classiques (exclure les zones QR déjà traitées)"

injection_code = '''
        # 1.5. Détection Clé-Valeur (Ancres textuelles pour les champs)
        zones_cle_valeur = {k: v for k, v in zones_config.items() if v.get('type') == 'ancre' and v.get('anchor_text')}
        if zones_cle_valeur:
            logger.info("🔍 Recherche par paires Clé-Valeur demandée...")
            if 'mots_ocr' not in locals() or mots_ocr is None or len(mots_ocr) == 0:
                mots_ocr, _ = ocr_global_avec_positions(image_path, lang='ara+fra')
            
            for nom_zone, config in zones_cle_valeur.items():
                anchor_text = config.get('anchor_text')
                direction = config.get('anchor_direction', 'gauche') # gauche, droite, bas, haut
                
                # Chercher le mot ancre
                meilleur_mot = None
                for mot in mots_ocr:
                    # Recherche exacte ou fuzzy basique
                    if anchor_text in mot['text'] or mot['text'] in anchor_text:
                        meilleur_mot = mot
                        break
                
                if meilleur_mot:
                    logger.info(f"✅ Mot ancre trouvé pour {nom_zone}: {meilleur_mot['text']}")
                    ax, ay, aw, ah = meilleur_mot['x'], meilleur_mot['y'], meilleur_mot['width'], meilleur_mot['height']
                    
                    # Trouver les mots dans la direction
                    mots_valeurs = []
                    for mot in mots_ocr:
                        if mot == meilleur_mot: continue
                        mx, my, mw, mh = mot['x'], mot['y'], mot['width'], mot['height']
                        
                        # Vérifier si sur la même ligne (tolérance Y)
                        if abs(my - ay) < ah:
                            if direction == 'gauche' and mx < ax:
                                mots_valeurs.append(mot)
                            elif direction == 'droite' and mx > ax:
                                mots_valeurs.append(mot)
                    
                    # Trier et fusionner
                    if mots_valeurs:
                        # Si direction gauche, trier de droite à gauche (RTL)
                        mots_valeurs.sort(key=lambda m: m['x'], reverse=(direction=='gauche'))
                        texte_extrait = " ".join([m['text'] for m in mots_valeurs])
                        
                        # Nettoyer si alpha_only
                        if config.get('char_filter') == 'alpha_only':
                            import re
                            texte_extrait = re.sub(r'[^\\w\\s\u0600-\u06FF]', '', texte_extrait)
                        
                        logger.info(f"🎉 Valeur extraite pour {nom_zone}: {texte_extrait}")
                        resultats[nom_zone] = {
                            'texte_auto': texte_extrait,
                            'confiance_auto': 0.9,
                            'statut': 'succes',
                            'moteur': 'cle-valeur',
                            'coords': config.get('coords', [0,0,1,1]),
                            'texte_final': texte_extrait
                        }
                else:
                    logger.warning(f"❌ Mot ancre non trouvé pour {nom_zone} ({anchor_text})")

        # 2. Zones OCR classiques (exclure les zones QR et Clé-Valeur déjà traitées)
'''

content = content.replace(injection_point, injection_code)

with open(engine_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Patch appliqué avec succès!")
