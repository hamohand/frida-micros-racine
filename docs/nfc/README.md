# Documentation NFC & lectures biométriques

Regroupement des documents liés à la stratégie de lecture des pièces d'identité (OCR, MRZ, NFC) dans FRIDA.

## Index

| Document | Phase | Objet |
|---|---|---|
| [lectures_biometriques.md](lectures_biometriques.md) | Vue d'ensemble | Stratégie en 3 phases : OCR+MRZ (déployé), lecteur NFC USB en cabinet, application mobile compagnon |
| [etude_lecteur_nfc.md](etude_lecteur_nfc.md) | Phase 2 | Étude technique intégration d'un lecteur NFC USB de bureau (ACR122U, PC/SC, ICAO 9303, BAC/PACE) |
| [architecture_app_mobile_nfc.md](architecture_app_mobile_nfc.md) | Phase 3 | Architecture de l'application mobile Flutter (scan MRZ caméra + lecture NFC + transmission backend) |

## Code lié

- `frida_mobile_nfc/` — projet Flutter de l'app mobile compagnon (Phase 3)
- Backend : route d'ingestion mobile-NFC (voir architecture_app_mobile_nfc.md §3)
