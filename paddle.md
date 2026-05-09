# PaddleOCR

Développé par Baidu via le framework **PaddlePaddle**. C'est l'un des meilleurs OCR open source disponibles, surtout pour les langues asiatiques, mais très performant en multilangue.

## Versions

- **PaddleOCR 2.x** — branche stable historique, la plus documentée
- **PaddleOCR 2.7.x** — dernière de la série 2.x
- **PaddleOCR 3.x** — en cours de maturation, architecture remaniée (PP-OCRv4, PP-StructureV2)
- Le modèle phare est **PP-OCRv4** (2023), nettement plus précis que v3

## Compatibilité Python

| PaddlePaddle | Python supporté |
|---|---|
| 2.5.x / 2.6.x | **3.8, 3.9, 3.10, 3.11** |
| < 2.4 | 3.7+ |

**Python 3.12 non supporté officiellement** — rester sur 3.10 ou 3.11.

## Installation

```bash
# 1. Installer PaddlePaddle (CPU)
pip install paddlepaddle

# GPU (CUDA 11.x)
pip install paddlepaddle-gpu

# 2. Installer PaddleOCR
pip install paddleocr
```

## Points forts vs concurrents

| | PaddleOCR | EasyOCR | Tesseract |
|---|---|---|---|
| Précision | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ |
| Vitesse CPU | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| Vitesse GPU | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | N/A |
| Layouts complexes | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ |
| Facilité d'install | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Taille des modèles | Lourds | Moyens | Léger |

## Langues supportées

Plus de **80 langues** dont français, arabe, anglais, chinois, japonais, coréen, etc. Les modèles sont téléchargés automatiquement à la première utilisation.

## Usage minimal

```python
from paddleocr import PaddleOCR

ocr = PaddleOCR(use_angle_cls=True, lang='fr')
result = ocr.ocr('image.png', cls=True)

for line in result[0]:
    print(line[1][0])  # texte extrait
```

## Limitations connues

- **Installation parfois capricieuse** sur Windows (dépendances C++, CUDA)
- **Python 3.12 non supporté** — rester sur 3.10 ou 3.11
- **Modèles volumineux** téléchargés au premier run (~100-500 MB selon config)
- **PaddlePaddle** est moins populaire que PyTorch dans l'écosystème occidental → moins de ressources communautaires
- Sur CPU, la première inférence est lente (chargement modèle)

## Recommandation pour documents de succession français

PaddleOCR PP-OCRv4 en `lang='fr'` avec `use_angle_cls=True` sera meilleur qu'EasyOCR sur des documents structurés (CNI, actes d'état civil). Utiliser Python 3.10 ou 3.11 pour éviter les problèmes de compatibilité.
