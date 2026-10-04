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
import com.muhend.backendai.service.ParametreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class WordGenerationService {

    private final ParametreService parametreService;

    public byte[] genererFridaWord(FridaEntity frida) throws IOException {
        // 1. Préparation des données pour le template
        Map<String, Object> data = new HashMap<>();

        // Paramètres de l'étude
        data.put("notaire", parametreService.getNomNotaireArabe());
        data.put("commune", parametreService.getCommuneEtude());
        data.put("wilaya", parametreService.getWilayaEtude());

        // Infos Frida
        data.put("num_frida", frida.getNumFrida() != null ? frida.getNumFrida() : "");
        data.put("date_creation", frida.getDateCreation() != null ? frida.getDateCreation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");

        // Textes dynamiques selon le sexe du défunt
        boolean isFeminin = false;
        if (frida.getDefunt() != null && frida.getDefunt().getIdentite() != null && "أنثى".equals(frida.getDefunt().getIdentite().getSexe())) {
            isFeminin = true;
        }
        
        data.put("defunt_titre", isFeminin ? "المرحومة" : "المرحوم");
        data.put("defunt_mort", isFeminin ? "المتوفية" : "المتوفي");
        data.put("defunt_ne", isFeminin ? "المولودة" : "المولود");
        data.put("defunt_fils", isFeminin ? "بنت" : "ابن");

        // Infos Défunt
        if (frida.getDefunt() != null) {
            IdentitesEntity identite = frida.getDefunt().getIdentite();
            data.put("defunt_nom", identite.getNom());
            data.put("defunt_prenom", identite.getPrenom());
            data.put("defunt_pere", identite.getPere() != null ? identite.getPere() : "");
            data.put("defunt_mere", identite.getMere() != null ? identite.getMere() : "");
            data.put("defunt_date_naiss", identite.getDateNaissance() != null ? identite.getDateNaissance().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
            data.put("defunt_lieu_naiss", identite.getLieuNaissance());
            data.put("defunt_date_deces", frida.getDefunt().getDateDeces() != null ? frida.getDefunt().getDateDeces().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
            data.put("defunt_sexe", identite.getSexe());
        }

        // Infos Héritiers (pour un tableau)
        List<Map<String, String>> heritiersList = new ArrayList<>();
        if (frida.getHeritiers() != null) {
            for (HeritierEntity heritier : frida.getHeritiers()) {
                // On exclut les témoins (souvent code 00)
                if ("00".equals(heritier.getNumParente())) continue;

                Map<String, String> hData = new HashMap<>();
                hData.put("h_nom", heritier.getIdentite().getNom());
                hData.put("h_prenom", heritier.getIdentite().getPrenom());
                hData.put("h_date_naiss", heritier.getIdentite().getDateNaissance() != null ? heritier.getIdentite().getDateNaissance().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
                hData.put("h_lieu_naiss", heritier.getIdentite().getLieuNaissance() != null ? heritier.getIdentite().getLieuNaissance() : "");
                hData.put("h_relation_exacte", getRelationArabe(heritier, frida));
                boolean isMaleH = isMasculin(heritier.getIdentite() != null ? heritier.getIdentite().getSexe() : "");
                hData.put("h_ne", isMaleH ? "المولود في" : "المولودة في");
                
                String nin = heritier.getIdentite() != null ? heritier.getIdentite().getNin() : "";
                String ninText = "";
                if (nin != null && !nin.isEmpty()) {
                    ninText = (isMaleH ? "، الحامل للرقم الوطني " : "، الحاملة للرقم الوطني ") + nin;
                }
                hData.put("h_nin_text", ninText);
                
                hData.put("h_part", getExactPart(heritier, frida.getCalcul()));
                heritiersList.add(hData);
            }
        }
        // Infos Témoins
        List<Map<String, String>> temoinsList = new ArrayList<>();
        if (frida.getHeritiers() != null) {
            for (HeritierEntity heritier : frida.getHeritiers()) {
                if ("00".equals(heritier.getNumParente())) {
                    Map<String, String> tData = new HashMap<>();
                    boolean isMaleT = isMasculin(heritier.getIdentite().getSexe());
                    tData.put("t_genre", isMaleT ? "السيد" : "السيدة");
                    tData.put("t_nom", heritier.getIdentite().getNom());
                    tData.put("t_prenom", heritier.getIdentite().getPrenom());
                    tData.put("t_nat", isMaleT ? "" : "ة");
                    tData.put("t_ne", isMaleT ? "المولود" : "المولودة");
                    tData.put("t_hab", isMaleT ? "الساكن" : "الساكنة");
                    tData.put("t_date_naiss", heritier.getIdentite().getDateNaissance() != null ? heritier.getIdentite().getDateNaissance().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
                    tData.put("t_adresse", heritier.getIdentite().getLieuNaissance() != null ? heritier.getIdentite().getLieuNaissance() : ""); // En l'absence d'adresse dans IdentiteEntity, on fallback sur lieu de naissance ou vide
                    temoinsList.add(tData);
                }
            }
        }
        data.put("temoins", temoinsList);
        data.put("heritiers", heritiersList);

        // Nouveaux éléments pour la section "Parts"
        if (frida.getCalcul() != null) {
            data.put("denominateur", frida.getCalcul().getDenominateur() != null ? frida.getCalcul().getDenominateur() : "");
            // Pour faire simple on l'écrit en chiffre, l'Arabe complet nécessiterait le service Angular ou une lib
            data.put("denominateur_lettres", frida.getCalcul().getDenominateur() != null ? frida.getCalcul().getDenominateur().toString() : ""); 
            
            List<Map<String, String>> partsList = new ArrayList<>();
            
            if (frida.getCalcul().getNumerateurConjoint() != null && frida.getCalcul().getNumerateurConjoint() > 0) {
                Map<String, String> p = new HashMap<>();
                p.put("h_groupe", isFeminin ? "لأرملها" : "لأرملته");
                p.put("h_nom_prenom", "");
                p.put("h_num", frida.getCalcul().getNumerateurConjoint().toString());
                p.put("h_den", frida.getCalcul().getDenominateur().toString());
                partsList.add(p);
            }
            if (frida.getCalcul().getNumerateurGarcons() != null && frida.getCalcul().getNumerateurGarcons() > 0) {
                Map<String, String> p = new HashMap<>();
                p.put("h_groupe", "للأبناء الذكور");
                p.put("h_nom_prenom", "لكل واحد منهم");
                p.put("h_num", frida.getCalcul().getNumerateurGarcons().toString());
                p.put("h_den", frida.getCalcul().getDenominateur().toString());
                partsList.add(p);
            }
            if (frida.getCalcul().getNumerateurFilles() != null && frida.getCalcul().getNumerateurFilles() > 0) {
                Map<String, String> p = new HashMap<>();
                p.put("h_groupe", "للبنات");
                p.put("h_nom_prenom", "لكل واحدة منهن");
                p.put("h_num", frida.getCalcul().getNumerateurFilles().toString());
                p.put("h_den", frida.getCalcul().getDenominateur().toString());
                partsList.add(p);
            }
            if (frida.getCalcul().getNumerateurPere() != null && frida.getCalcul().getNumerateurPere() > 0) {
                Map<String, String> p = new HashMap<>();
                p.put("h_groupe", "للأب");
                p.put("h_nom_prenom", "");
                p.put("h_num", frida.getCalcul().getNumerateurPere().toString());
                p.put("h_den", frida.getCalcul().getDenominateur().toString());
                partsList.add(p);
            }
            if (frida.getCalcul().getNumerateurMere() != null && frida.getCalcul().getNumerateurMere() > 0) {
                Map<String, String> p = new HashMap<>();
                p.put("h_groupe", "للأم");
                p.put("h_nom_prenom", "");
                p.put("h_num", frida.getCalcul().getNumerateurMere().toString());
                p.put("h_den", frida.getCalcul().getDenominateur().toString());
                partsList.add(p);
            }
            
            // Frères, sœurs, oncles (individuellement)
            for (HeritierEntity h : frida.getHeritiers()) {
                if (h.getNumParente() != null && Integer.parseInt(h.getNumParente()) >= 5) {
                    String relArabe = getRelationArabePourPart(h, frida);
                    String partString = getExactPart(h, frida.getCalcul());
                    if (!"-".equals(partString) && !partString.isEmpty()) {
                        String[] parts = partString.split("/");
                        Map<String, String> p = new HashMap<>();
                        p.put("h_groupe", relArabe);
                        p.put("h_nom_prenom", h.getIdentite().getPrenom());
                        p.put("h_num", parts.length > 0 ? parts[0] : "");
                        p.put("h_den", parts.length > 1 ? parts[1] : "");
                        partsList.add(p);
                    }
                }
            }
            data.put("heritiers_parts", partsList);
            
            // Calcul du total des parts distribuées
            int totalNum = 0;
            if (frida.getHeritiers() != null) {
                for (HeritierEntity h : frida.getHeritiers()) {
                    String partString = getExactPart(h, frida.getCalcul());
                    if (!"-".equals(partString) && !partString.isEmpty()) {
                        String[] parts = partString.split("/");
                        if (parts.length > 0) {
                            try {
                                totalNum += Integer.parseInt(parts[0]);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
            data.put("total_num", String.valueOf(totalNum));
        }

        // 3. Rendu du document
        ClassPathResource resource = new ClassPathResource("templates/modele_frida.docx");
        
        try (InputStream is = resource.getInputStream();
             XWPFTemplate template = XWPFTemplate.compile(is).render(data);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // Protection du document en lecture seule (avec mot de passe arbitraire)
            XWPFDocument document = template.getXWPFDocument();
            document.enforceReadonlyProtection("frida2026", org.apache.poi.poifs.crypt.HashAlgorithm.sha256);
            
            template.write(out);
            return out.toByteArray();
        }
    }

    private String getExactPart(HeritierEntity heritier, com.muhend.backendai.entities.CalculEntity calcul) {
        if (calcul == null || calcul.getDenominateur() == null || calcul.getDenominateur() == 0) return "";
        int num = 0;
        int den = calcul.getDenominateur();
        
        String parent = heritier.getNumParente();
        String sexe = heritier.getIdentite() != null ? heritier.getIdentite().getSexe() : "";
        
        if ("02".equals(parent)) {
            num = calcul.getNumerateurConjoint() != null ? calcul.getNumerateurConjoint() : 0;
        } else if ("03".equals(parent)) {
            if ("ذكر".equals(sexe)) {
                num = calcul.getNumerateurGarcons() != null ? calcul.getNumerateurGarcons() : 0;
            } else {
                num = calcul.getNumerateurFilles() != null ? calcul.getNumerateurFilles() : 0;
            }
        } else if ("04".equals(parent)) {
            if ("ذكر".equals(sexe)) {
                num = calcul.getNumerateurPere() != null ? calcul.getNumerateurPere() : 0;
            } else {
                num = calcul.getNumerateurMere() != null ? calcul.getNumerateurMere() : 0;
            }
        } else if ("05".equals(parent) || "06".equals(parent) || "07".equals(parent)) {
            if ("ذكر".equals(sexe)) {
                num = calcul.getNumerateurFreres() != null ? calcul.getNumerateurFreres() : 0;
            } else {
                num = calcul.getNumerateurSoeurs() != null ? calcul.getNumerateurSoeurs() : 0;
            }
        } else if ("08".equals(parent)) {
            num = calcul.getNumerateurOnclesPaternels() != null ? calcul.getNumerateurOnclesPaternels() : 0;
        } else if ("09".equals(parent)) {
            num = calcul.getNumerateurPetitsFils() != null ? calcul.getNumerateurPetitsFils() : 0;
        } else if ("10".equals(parent)) {
            num = calcul.getNumerateurPetitesFilles() != null ? calcul.getNumerateurPetitesFilles() : 0;
        } else if ("11".equals(parent)) {
            num = calcul.getNumerateurCousinsPaternels() != null ? calcul.getNumerateurCousinsPaternels() : 0;
        } else if ("12".equals(parent)) {
            num = calcul.getNumerateurGrandPerePaternel() != null ? calcul.getNumerateurGrandPerePaternel() : 0;
        } else if ("13".equals(parent)) {
            num = calcul.getNumerateurGrandMerePaternelle() != null ? calcul.getNumerateurGrandMerePaternelle() : 0;
        }
        
        if (num == 0) return "-";
        return num + "/" + den;
    }

    private boolean isMasculin(String sexe) {
        if (sexe == null) return false;
        String s = sexe.trim();
        return s.equals("ذكر") || s.equals("ذ") || s.equalsIgnoreCase("M");
    }

    private String getRelationArabe(HeritierEntity heritier, FridaEntity frida) {
        boolean isMaleDefunt = isMasculin(frida.getDefunt() != null && frida.getDefunt().getIdentite() != null ? frida.getDefunt().getIdentite().getSexe() : "");
        String numParente = heritier.getNumParente();
        boolean isMaleHeritier = isMasculin(heritier.getIdentite() != null ? heritier.getIdentite().getSexe() : "");

        if ("02".equals(numParente)) {
            return isMaleDefunt ? "أرملته" : "أرملها";
        } else if ("03".equals(numParente)) {
            if (isMaleDefunt) return isMaleHeritier ? "ابنه" : "بنته";
            else return isMaleHeritier ? "ابنها" : "بنتها";
        } else if ("04".equals(numParente)) {
            if (isMaleDefunt) return isMaleHeritier ? "والده" : "والدته";
            else return isMaleHeritier ? "والدها" : "والدتها";
        } else if ("05".equals(numParente)) {
            if (isMaleDefunt) return isMaleHeritier ? "أخوه" : "أخته";
            else return isMaleHeritier ? "أخوها" : "أختها";
        } else if ("06".equals(numParente)) {
            return isMaleDefunt ? "عمه" : "عمها";
        } else if ("07".equals(numParente)) {
            return isMaleDefunt ? "ابن عمه" : "ابن عمها";
        } else if ("08".equals(numParente)) {
            return isMaleDefunt ? "جده" : "جدها";
        }
        return heritier.getCategorieArabe() != null ? heritier.getCategorieArabe() : "";
    }

    private String getRelationArabePourPart(HeritierEntity heritier, FridaEntity frida) {
        boolean isMaleDefunt = isMasculin(frida.getDefunt() != null && frida.getDefunt().getIdentite() != null ? frida.getDefunt().getIdentite().getSexe() : "");
        String numParente = heritier.getNumParente();
        boolean isMaleHeritier = isMasculin(heritier.getIdentite() != null ? heritier.getIdentite().getSexe() : "");

        if ("05".equals(numParente)) {
            if (isMaleDefunt) return isMaleHeritier ? "لأخيه" : "لأخته";
            else return isMaleHeritier ? "لأخيها" : "لأختها";
        } else if ("06".equals(numParente)) {
            return isMaleDefunt ? "لعمه" : "لعمها";
        } else if ("07".equals(numParente)) {
            return isMaleDefunt ? "لابن عمه" : "لابن عمها";
        }
        return getRelationArabe(heritier, frida);
    }
}
