package com.muhend.backendai.service.document;

import fr.opensagres.poi.xwpf.converter.pdf.PdfConverter;
import fr.opensagres.poi.xwpf.converter.pdf.PdfOptions;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import java.io.ByteArrayInputStream;
import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.plugin.table.LoopRowTableRenderPolicy;
import com.muhend.backendai.entities.FridaEntity;
import com.muhend.backendai.entities.IdentitesEntity;
import com.muhend.backendai.entities.HeritierEntity;
import com.muhend.backendai.entities.TemoinEntity;
import com.muhend.backendai.service.HeritierService;
import com.muhend.backendai.service.ParametreService;
import com.muhend.backendai.service.TemoinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Profile("!calc-only")
@Service
@Slf4j
@RequiredArgsConstructor
public class WordGenerationService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final ParametreService parametreService;
    private final HeritierService heritierService;
    private final TemoinService temoinService;
    private final com.muhend.backendai.repository.IdentitesRepo identitesRepo;

    @org.springframework.beans.factory.annotation.Value("${app.demo-bridee:false}")
    private boolean demoBridee;

    public void setDemoBridee(boolean demoBridee) {
        this.demoBridee = demoBridee;
    }

    public byte[] genererFridaWord(FridaEntity frida) throws IOException {
        Map<String, Object> data = new HashMap<>();

        // 1. Paramètres de l'étude (En-tête : nom du notaire, rue et ville)
        String nomNotaire = parametreService.getNomNotaireArabe();
        if (nomNotaire == null || nomNotaire.isBlank()) {
            nomNotaire = frida.getNotaire() != null ? frida.getNotaire() : "";
        }
        String adresseEtude = parametreService.getAdresseEtude();
        String communeEtude = parametreService.getCommuneEtude();
        String wilayaEtude = parametreService.getWilayaEtude();

        data.put("notaire", nomNotaire);
        data.put("adresse", adresseEtude != null ? adresseEtude.trim() : "");
        data.put("commune", communeEtude != null ? communeEtude.trim() : "");
        data.put("wilaya", wilayaEtude != null ? wilayaEtude.trim() : "");

        List<String> lignesAdresse = new ArrayList<>();
        if (adresseEtude != null && !adresseEtude.isBlank()) {
            lignesAdresse.add(adresseEtude.trim());
        }
        List<String> villeParts = new ArrayList<>();
        if (communeEtude != null && !communeEtude.isBlank()) {
            villeParts.add("بلدية " + communeEtude.trim());
        }
        if (wilayaEtude != null && !wilayaEtude.isBlank()) {
            villeParts.add("ولاية " + wilayaEtude.trim());
        }
        if (!villeParts.isEmpty()) {
            lignesAdresse.add(String.join(" - ", villeParts));
        }
        data.put("notaire_adresse", String.join("\n", lignesAdresse));

        // 2. Infos Frida
        String numFrida = frida.getNumFrida() != null ? frida.getNumFrida() : "";
        data.put("num_frida", numFrida);
        data.put("date_creation", formatDate(frida.getDateCreation()));

        // 3. Infos Défunt (alignées exactement sur frida.component.html)
        boolean isMaleDefunt = frida.getDefunt() != null
                && frida.getDefunt().getIdentite() != null
                && isMasculin(frida.getDefunt().getIdentite().getSexe());
        boolean isFeminin = !isMaleDefunt;

        data.put("defunt_titre", isFeminin ? "المرحومة" : "المرحوم");
        data.put("defunt_pour_temoins", isFeminin ? "للمرحومة" : "للمرحوم");
        data.put("defunt_mort", isFeminin ? "المتوفية" : "المتوفي");
        data.put("defunt_ne", isFeminin ? "المولودة" : "المولود");
        data.put("defunt_fils", isFeminin ? "بنت" : "بن");

        if (frida.getDefunt() != null && frida.getDefunt().getIdentite() != null) {
            IdentitesEntity identite = frida.getDefunt().getIdentite();
            data.put("defunt_nom", safeStr(identite.getNom()));
            data.put("defunt_prenom", safeStr(identite.getPrenom()));
            data.put("defunt_pere", safeStr(identite.getPere()));
            data.put("defunt_mere", safeStr(identite.getMere()));
            data.put("defunt_date_naiss", formatDate(identite.getDateNaissance()));
            data.put("defunt_lieu_naiss", safeStr(identite.getLieuNaissance()));
            data.put("defunt_date_deces", formatDate(frida.getDefunt().getDateDeces()));
            data.put("defunt_sexe", safeStr(identite.getSexe()));

            String wilayaDef = safeStr(identite.getWilaya());
            data.put("defunt_wilaya_text", !wilayaDef.isEmpty() ? " (ولاية " + wilayaDef + ")" : "");

            String ninDef = safeStr(identite.getNin());
            if (!ninDef.isEmpty()) {
                data.put("defunt_nin_text", " " + (isFeminin ? "الحاملة" : "الحامل")
                        + " لرقم التعريف الوطني (NIN) : " + ninDef + "،");
            } else {
                data.put("defunt_nin_text", "");
            }

            String dateDecesStr = formatDate(frida.getDefunt().getDateDeces());
            if (!dateDecesStr.isEmpty()) {
                data.put("defunt_deces_text", " " + (isFeminin ? "المتوفية" : "المتوفي")
                        + " بتاريخ " + dateDecesStr + "،");
            } else {
                data.put("defunt_deces_text", "");
            }

            String numPieceDef = safeStr(identite.getNumeroPiece());
            if (!numPieceDef.isEmpty()) {
                data.put("defunt_acte_text", " بناءً على شهادة " + (isFeminin ? "ميلادها" : "ميلاده")
                        + " رقم " + numPieceDef + ".");
            } else {
                data.put("defunt_acte_text", ".");
            }
        } else {
            data.put("defunt_nom", "");
            data.put("defunt_prenom", "");
            data.put("defunt_pere", "");
            data.put("defunt_mere", "");
            data.put("defunt_date_naiss", "");
            data.put("defunt_lieu_naiss", "");
            data.put("defunt_date_deces", "");
            data.put("defunt_sexe", "");
            data.put("defunt_wilaya_text", "");
            data.put("defunt_nin_text", "");
            data.put("defunt_deces_text", "");
            data.put("defunt_acte_text", ".");
        }

        // 4. Récupération des héritiers (ordonnés comme dans la vue web) et des témoins
        List<HeritierEntity> heritiersOrdered = new ArrayList<>();
        if (!numFrida.isEmpty()) {
            try {
                List<HeritierEntity> fromRepo = heritierService.listeHeritiers(numFrida);
                if (fromRepo != null && !fromRepo.isEmpty()) {
                    heritiersOrdered = fromRepo;
                }
            } catch (Exception e) {
                log.warn("Impossible de charger listeHeritiers via HeritierService pour {}", numFrida, e);
            }
        }
        if (heritiersOrdered.isEmpty() && frida.getHeritiers() != null) {
            heritiersOrdered = frida.getHeritiers();
        }

        List<TemoinEntity> temoinsEntities = new ArrayList<>();
        if (!numFrida.isEmpty()) {
            try {
                List<TemoinEntity> fromRepo = temoinService.listeTemoins(numFrida);
                if (fromRepo != null && !fromRepo.isEmpty()) {
                    temoinsEntities = fromRepo;
                }
            } catch (Exception e) {
                log.warn("Impossible de charger listeTemoins via TemoinService pour {}", numFrida, e);
            }
        }
        if (temoinsEntities.isEmpty() && frida.getTemoins() != null) {
            temoinsEntities = frida.getTemoins();
        }

        // 5. Liste des témoins
        List<Map<String, String>> temoinsList = new ArrayList<>();
        int tIdx = 1;
        for (TemoinEntity temoin : temoinsEntities) {
            IdentitesEntity idT = temoin.getIdentite();
            boolean isMaleT = idT != null && isMasculin(idT.getSexe());
            Map<String, String> tData = new HashMap<>();
            tData.put("t_index", String.valueOf(tIdx++));
            tData.put("t_genre", isMaleT ? "السيد" : "السيدة");
            tData.put("t_nom", idT != null ? safeStr(idT.getNom()) : "");
            tData.put("t_prenom", idT != null ? safeStr(idT.getPrenom()) : "");
            tData.put("t_nat", isMaleT ? "" : "ة");
            tData.put("t_ne", isMaleT ? "المولود" : "المولودة");
            tData.put("t_hab", isMaleT ? "الساكن" : "الساكنة");
            tData.put("t_date_naiss", idT != null ? formatDate(idT.getDateNaissance()) : "");
            String adr = safeStr(temoin.getAdresse());
            if (adr.isEmpty() && idT != null) {
                adr = safeStr(idT.getLieuNaissance());
            }
            tData.put("t_adresse", adr);
            temoinsList.add(tData);
        }
        data.put("temoins", temoinsList);

        // 6. 1ère liste nominative des héritiers (الـورثـة : parenté, nom, prénom, date et lieu de naissance, NIN - sans fraction)
        List<Map<String, String>> heritiersList = new ArrayList<>();
        for (HeritierEntity heritier : heritiersOrdered) {
            if ("00".equals(heritier.getNumParente())) continue;
            IdentitesEntity idH = heritier.getIdentite();
            boolean isMaleH = idH != null && isMasculin(idH.getSexe());

            Map<String, String> hData = new HashMap<>();
            hData.put("h_relation_exacte", getRelationArabe(heritier, frida) + " :");
            hData.put("h_nom", idH != null ? safeStr(idH.getNom()) : "");
            hData.put("h_prenom", idH != null ? safeStr(idH.getPrenom()) : "");
            hData.put("h_ne", isMaleH ? "المولود بتاريخ" : "المولودة بتاريخ");
            hData.put("h_date_naiss", idH != null ? formatDate(idH.getDateNaissance()) : "");

            String lieuNaiss = idH != null ? safeStr(idH.getLieuNaissance()) : "";
            if (lieuNaiss.isEmpty() && idH != null) {
                lieuNaiss = resolveLieuNaissanceFallback(idH);
            }
            hData.put("h_lieu_prefix", !lieuNaiss.isEmpty() ? " بـ " : "");
            hData.put("h_lieu_naiss", lieuNaiss);

            StringBuilder details = new StringBuilder();
            if (!lieuNaiss.isEmpty()) {
                // lieuNaiss est affiché en gras via {{h_lieu_prefix}}{{h_lieu_naiss}} dans modele_frida.docx
            }
            String nin = idH != null ? safeStr(idH.getNin()) : "";
            if (!nin.isEmpty()) {
                details.append(isMaleH ? "، الحامل للرقم الوطني " : "، الحاملة للرقم الوطني ").append(nin);
            }
            hData.put("h_nin_text", details.toString());
            hData.put("h_part", "");
            heritiersList.add(hData);
        }
        data.put("heritiers", heritiersList);

        // 7. 2ème liste nominative des héritiers (الـقـسـمـة : parenté, nom, prénom, traits d'union serrés et fraction en fin de ligne)
        if (frida.getCalcul() != null) {
            Integer denVal = frida.getCalcul().getDenominateur();
            data.put("denominateur", denVal != null ? denVal : "");
            data.put("denominateur_lettres", denVal != null ? com.muhend.backendai.service.calculs_outils.NumberToArabicWords.convert(denVal) : "");

            // Demandeur de la Frida (même règle que frida.component.html lignes 181-189)
            HeritierEntity demandeur = null;
            boolean hasChildren = (frida.getCalcul().getNbGarcons() != null && frida.getCalcul().getNbGarcons() != 0)
                    || (frida.getCalcul().getNbFilles() != null && frida.getCalcul().getNbFilles() != 0);
            if (hasChildren && heritiersOrdered.size() > 1) {
                demandeur = heritiersOrdered.get(1);
            } else if (!heritiersOrdered.isEmpty()) {
                demandeur = heritiersOrdered.get(0);
            }
            if (demandeur != null && demandeur.getIdentite() != null) {
                boolean isMaleDem = isMasculin(demandeur.getIdentite().getSexe());
                String titreDem = isMaleDem ? "السيد" : "السيدة";
                String nomPrenomDem = (safeStr(demandeur.getIdentite().getNom()) + " " + safeStr(demandeur.getIdentite().getPrenom())).trim();
                data.put("demandeur_intro", "وبناءً على طلب " + titreDem + " " + nomPrenomDem + "، إحدى الورثة المذكورين أعلاه، تم إعداد هذه الفريضة. ");
            } else {
                data.put("demandeur_intro", "");
            }

            List<Map<String, String>> partsList = new ArrayList<>();
            int totalNum = 0;
            for (HeritierEntity h : heritiersOrdered) {
                if ("00".equals(h.getNumParente())) continue;
                String relArabe = getRelationArabePourPart(h, frida);
                String partString = getExactPart(h, frida.getCalcul());
                String nomH = h.getIdentite() != null ? safeStr(h.getIdentite().getNom()) : "";
                String prenomH = h.getIdentite() != null ? safeStr(h.getIdentite().getPrenom()) : "";

                Map<String, String> p = new HashMap<>();
                p.put("h_groupe", relArabe + " :");
                p.put("h_nom_prenom", (nomH + " " + prenomH).trim());

                if (!"-".equals(partString) && !partString.isEmpty()) {
                    String[] parts = partString.split("/");
                    String numStr = parts.length > 0 ? parts[0] : "";
                    String denStr = parts.length > 1 ? parts[1] : "";
                    p.put("h_num", numStr);
                    p.put("h_den", denStr);
                    p.put("h_fraction", "\u00A0\u200E" + numStr + "\u00A0/\u00A0" + denStr + "\u200E");
                    try {
                        totalNum += Integer.parseInt(numStr);
                    } catch (NumberFormatException ignored) {}
                } else {
                    p.put("h_num", "-");
                    p.put("h_den", "");
                    p.put("h_fraction", "-");
                }
                partsList.add(p);
            }
            data.put("heritiers_parts", partsList);
            data.put("total_num", String.valueOf(totalNum));
            data.put("total_fraction", "\u00A0\u200E" + totalNum + "\u00A0/\u00A0" + (denVal != null ? denVal : "") + "\u200E");
            data.put("total_num_lettres", com.muhend.backendai.service.calculs_outils.NumberToArabicWords.convert(totalNum));
        }

        // 8. Rendu du document Word
        ClassPathResource resource = new ClassPathResource("templates/modele_frida.docx");

        try (InputStream is = resource.getInputStream();
             XWPFTemplate template = XWPFTemplate.compile(is).render(data);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Marges du document : droite 5 cm (2835 twips), gauche 1 cm (567 twips)
            XWPFDocument document = template.getXWPFDocument();
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr sectPr = document.getDocument().getBody().isSetSectPr()
                    ? document.getDocument().getBody().getSectPr()
                    : document.getDocument().getBody().addNewSectPr();
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar pageMar = sectPr.isSetPgMar()
                    ? sectPr.getPgMar()
                    : sectPr.addNewPgMar();
            pageMar.setRight(java.math.BigInteger.valueOf(2835L)); // 5 cm
            pageMar.setLeft(java.math.BigInteger.valueOf(567L));   // 1 cm
            if (pageMar.getTop() == null) pageMar.setTop(java.math.BigInteger.valueOf(1134L));       // 2 cm
            if (pageMar.getBottom() == null) pageMar.setBottom(java.math.BigInteger.valueOf(1134L)); // 2 cm

            if (demoBridee) {
                appliquerFiligraneWord(document, "SPECIMEN");
            }

            // Protection du document en lecture seule (avec mot de passe arbitraire)
            document.enforceReadonlyProtection("frida2026", org.apache.poi.poifs.crypt.HashAlgorithm.sha256);

            template.write(out);
            return out.toByteArray();
        }
    }

    public byte[] genererFridaPdf(FridaEntity frida) throws IOException {
        byte[] wordBytes = genererFridaWord(frida);
        try (ByteArrayInputStream in = new ByteArrayInputStream(wordBytes);
             XWPFDocument document = new XWPFDocument(in);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PdfOptions options = PdfOptions.create();
            PdfConverter.getInstance().convert(document, out, options);
            byte[] pdfBytes = out.toByteArray();

            if (demoBridee) {
                return appliquerFiligranePdf(pdfBytes, "SPECIMEN");
            }
            return pdfBytes;
        } catch (Exception e) {
            log.error("Erreur lors de la conversion du document Word en PDF", e);
            throw new IOException("Impossible de générer le PDF", e);
        }
    }

    private void appliquerFiligraneWord(XWPFDocument document, String texte) {
        try {
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr sectPr =
                    document.getDocument().getBody().isSetSectPr()
                            ? document.getDocument().getBody().getSectPr()
                            : document.getDocument().getBody().addNewSectPr();
            org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy policy =
                    new org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy(document, sectPr);
            policy.createWatermark(texte);
        } catch (Exception e) {
            log.warn("Impossible d'appliquer le filigrane Word '{}' : {}", texte, e.getMessage());
        }
    }

    private byte[] appliquerFiligranePdf(byte[] pdfBytes, String texte) {
        try (ByteArrayOutputStream stampedOut = new ByteArrayOutputStream()) {
            com.lowagie.text.pdf.PdfReader reader = new com.lowagie.text.pdf.PdfReader(pdfBytes);
            com.lowagie.text.pdf.PdfStamper stamper = new com.lowagie.text.pdf.PdfStamper(reader, stampedOut);
            com.lowagie.text.pdf.BaseFont font = com.lowagie.text.pdf.BaseFont.createFont(
                    com.lowagie.text.pdf.BaseFont.HELVETICA_BOLD,
                    com.lowagie.text.pdf.BaseFont.WINANSI,
                    com.lowagie.text.pdf.BaseFont.NOT_EMBEDDED);
            com.lowagie.text.pdf.PdfGState gs = new com.lowagie.text.pdf.PdfGState();
            gs.setFillOpacity(0.18f);

            int totalPages = reader.getNumberOfPages();
            for (int i = 1; i <= totalPages; i++) {
                com.lowagie.text.Rectangle pageSize = reader.getPageSizeWithRotation(i);
                float x = (pageSize.getLeft() + pageSize.getRight()) / 2f;
                float y = (pageSize.getBottom() + pageSize.getTop()) / 2f;
                com.lowagie.text.pdf.PdfContentByte over = stamper.getOverContent(i);
                over.saveState();
                over.setGState(gs);
                over.beginText();
                over.setFontAndSize(font, 72);
                over.setRGBColorFill(180, 30, 30);
                over.showTextAligned(com.lowagie.text.Element.ALIGN_CENTER, texte, x, y, 45);
                over.endText();
                over.restoreState();
            }
            stamper.close();
            reader.close();
            return stampedOut.toByteArray();
        } catch (Exception e) {
            log.warn("Impossible d'appliquer le filigrane PDF '{}' : {}", texte, e.getMessage());
            return pdfBytes;
        }
    }

    private String safeStr(String s) {
        return s != null ? s.trim() : "";
    }

    private String resolveLieuNaissanceFallback(IdentitesEntity idH) {
        try {
            if (identitesRepo == null) return "";
            String nin = safeStr(idH.getNin());
            if (!nin.isEmpty()) {
                for (IdentitesEntity cand : identitesRepo.findByNin(nin)) {
                    String l = safeStr(cand.getLieuNaissance());
                    if (!l.isEmpty()) return l;
                }
            }
            String nom = safeStr(idH.getNom());
            String prenom = safeStr(idH.getPrenom());
            if (!nom.isEmpty() && !prenom.isEmpty()) {
                for (IdentitesEntity cand : identitesRepo.findByNomAndPrenom(nom, prenom)) {
                    String l = safeStr(cand.getLieuNaissance());
                    if (!l.isEmpty()) return l;
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    private String formatDate(LocalDate d) {
        return d != null ? d.format(DATE_FMT) : "";
    }

    private String getExactPart(HeritierEntity heritier, com.muhend.backendai.entities.CalculEntity calcul) {
        if (calcul == null || calcul.getDenominateur() == null || calcul.getDenominateur() == 0) return "";
        int num = 0;
        int den = calcul.getDenominateur();

        String parent = heritier.getNumParente() != null ? heritier.getNumParente().trim() : "";
        // Normalisation : accepte "2" ou "02"
        int codeParente = -1;
        try {
            codeParente = Integer.parseInt(parent);
        } catch (NumberFormatException ignored) {}

        String sexe = heritier.getIdentite() != null ? heritier.getIdentite().getSexe() : "";
        boolean isMale = isMasculin(sexe);

        if (codeParente == 2) {
            num = calcul.getNumerateurConjoint() != null ? calcul.getNumerateurConjoint() : 0;
        } else if (codeParente == 3) {
            if (isMale) {
                num = calcul.getNumerateurGarcons() != null ? calcul.getNumerateurGarcons() : 0;
            } else {
                num = calcul.getNumerateurFilles() != null ? calcul.getNumerateurFilles() : 0;
            }
        } else if (codeParente == 4) {
            if (isMale) {
                num = calcul.getNumerateurPere() != null ? calcul.getNumerateurPere() : 0;
            } else {
                num = calcul.getNumerateurMere() != null ? calcul.getNumerateurMere() : 0;
            }
        } else if (codeParente == 5) {
            if (isMale) {
                num = calcul.getNumerateurFreres() != null ? calcul.getNumerateurFreres() : 0;
            } else {
                num = calcul.getNumerateurSoeurs() != null ? calcul.getNumerateurSoeurs() : 0;
            }
        } else if (codeParente == 6) {
            num = calcul.getNumerateurOnclesPaternels() != null ? calcul.getNumerateurOnclesPaternels() : 0;
        } else if (codeParente == 7) {
            num = calcul.getNumerateurCousinsPaternels() != null ? calcul.getNumerateurCousinsPaternels() : 0;
        } else if (codeParente == 8) {
            num = calcul.getNumerateurGrandPerePaternel() != null ? calcul.getNumerateurGrandPerePaternel() : 0;
        } else if (codeParente == 9 || codeParente == 10) {
            if (isMale) {
                num = calcul.getNumerateurPetitsFils() != null ? calcul.getNumerateurPetitsFils() : 0;
            } else {
                num = calcul.getNumerateurPetitesFilles() != null ? calcul.getNumerateurPetitesFilles() : 0;
            }
        } else if (codeParente == 11) {
            num = calcul.getNumerateurGrandMerePaternelle() != null ? calcul.getNumerateurGrandMerePaternelle() : 0;
        }

        if (num == 0 && heritier.getCoefPart() > 0) {
            num = (int) Math.round(heritier.getCoefPart() * den);
        }

        if (num == 0) return "-";
        return num + "/" + den;
    }

    private boolean isMasculin(String sexe) {
        if (sexe == null) return false;
        String s = sexe.trim();
        return s.equals("ذكر") || s.equals("ذ") || s.equalsIgnoreCase("M");
    }

    private int parseParente(String numParente) {
        if (numParente == null) return -1;
        try {
            return Integer.parseInt(numParente.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String getRelationArabe(HeritierEntity heritier, FridaEntity frida) {
        boolean isMaleDefunt = isMasculin(frida.getDefunt() != null && frida.getDefunt().getIdentite() != null ? frida.getDefunt().getIdentite().getSexe() : "");
        int code = parseParente(heritier.getNumParente());
        boolean isMaleHeritier = isMasculin(heritier.getIdentite() != null ? heritier.getIdentite().getSexe() : "");

        if (code == 2) {
            return isMaleDefunt ? "أرملته" : "أرملها";
        } else if (code == 3) {
            if (isMaleDefunt) return isMaleHeritier ? "ابنه" : "بنته";
            else return isMaleHeritier ? "ابنها" : "بنتها";
        } else if (code == 4) {
            if (isMaleDefunt) return isMaleHeritier ? "والده" : "والدته";
            else return isMaleHeritier ? "والدها" : "والدتها";
        } else if (code == 5) {
            if (isMaleDefunt) return isMaleHeritier ? "أخوه" : "أخته";
            else return isMaleHeritier ? "أخوها" : "أختها";
        } else if (code == 6) {
            return isMaleDefunt ? "عمه" : "عمها";
        } else if (code == 7) {
            return isMaleDefunt ? "ابن عمه" : "ابن عمها";
        } else if (code == 8) {
            return isMaleDefunt ? "جده" : "جدها";
        } else if (code == 9 || code == 10) {
            if (isMaleDefunt) return isMaleHeritier ? "حفيده" : "حفيدته";
            else return isMaleHeritier ? "حفيدها" : "حفيدتها";
        } else if (code == 11) {
            return isMaleDefunt ? "جدته" : "جدتها";
        }
        return heritier.getCategorieArabe() != null ? heritier.getCategorieArabe() : "";
    }

    private String getRelationArabePourPart(HeritierEntity heritier, FridaEntity frida) {
        boolean isMaleDefunt = isMasculin(frida.getDefunt() != null && frida.getDefunt().getIdentite() != null ? frida.getDefunt().getIdentite().getSexe() : "");
        int code = parseParente(heritier.getNumParente());
        boolean isMaleHeritier = isMasculin(heritier.getIdentite() != null ? heritier.getIdentite().getSexe() : "");

        if (code == 2) {
            return isMaleDefunt ? "لأرملته" : "لأرملها";
        } else if (code == 3) {
            if (isMaleDefunt) return isMaleHeritier ? "لابنه" : "لبنته";
            else return isMaleHeritier ? "لابنها" : "لبنتها";
        } else if (code == 4) {
            if (isMaleDefunt) return isMaleHeritier ? "لوالده" : "لوالدته";
            else return isMaleHeritier ? "لوالدها" : "لوالدتها";
        } else if (code == 5) {
            if (isMaleDefunt) return isMaleHeritier ? "لأخيه" : "لأخته";
            else return isMaleHeritier ? "لأخيها" : "لأختها";
        } else if (code == 6) {
            return isMaleDefunt ? "لعمه" : "لعمها";
        } else if (code == 7) {
            return isMaleDefunt ? "لابن عمه" : "لابن عمها";
        } else if (code == 8) {
            return isMaleDefunt ? "لجده" : "لجدها";
        } else if (code == 9 || code == 10) {
            if (isMaleDefunt) return isMaleHeritier ? "لحفيده" : "لحفيدته";
            else return isMaleHeritier ? "لحفيدها" : "لحفيدتها";
        } else if (code == 11) {
            return isMaleDefunt ? "لجدته" : "لجدتها";
        }
        return getRelationArabe(heritier, frida);
    }
}

