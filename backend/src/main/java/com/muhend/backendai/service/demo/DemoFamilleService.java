package com.muhend.backendai.service.demo;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Gère le dossier en ligne « famille » contenant les actes fictifs autorisés
 * en mode démonstration bridée (DEMO_BRIDEE=true) et vérifie par empreinte SHA-256
 * que tout fichier téléversé provient exclusivement de ce dossier.
 */
@Service
@Slf4j
public class DemoFamilleService {

    public static final String BASE_PATH = "demo-specimens/famille/";

    @Getter
    @RequiredArgsConstructor
    public static class FichierFamilleInfo {
        private final String nom;
        private final String libelleFr;
        private final String libelleAr;
        private final String typeActe; // "ad" (acte de décès) ou "en" (extrait de naissance)
        private final String categorie; // code indicatif ("01", "02", "03", "04", "05", "00")
    }

    private static final List<FichierFamilleInfo> CATALOGUE = List.of(
            new FichierFamilleInfo("epoux_deces.png", "Acte de décès — Époux (KACEM Ahmed)", "عقد وفاة — الزوج (قاسم أحمد)", "ad", "01"),
            new FichierFamilleInfo("epouse_deces.png", "Acte de décès — Épouse (NASOURI Zahra)", "عقد وفاة — الزوجة (نصوري زهرة)", "ad", "01"),
            new FichierFamilleInfo("epoux.png", "Acte de naissance — Époux (KACEM Ahmed)", "شهادة ميلاد — الزوج (قاسم أحمد)", "en", "01"),
            new FichierFamilleInfo("epouse.png", "Acte de naissance — Épouse (NASOURI Zahra)", "شهادة ميلاد — الزوجة (نصوري زهرة)", "en", "02"),
            new FichierFamilleInfo("fils_1.png", "Acte de naissance — Fils 1 (KACEM Mohamed)", "شهادة ميلاد — الابن 1 (قاسم محمد)", "en", "03"),
            new FichierFamilleInfo("fils_2.png", "Acte de naissance — Fils 2 (KACEM Youcef)", "شهادة ميلاد — الابن 2 (قاسم يوسف)", "en", "03"),
            new FichierFamilleInfo("fils_3.png", "Acte de naissance — Fils 3 (KACEM Karim)", "شهادة ميلاد — الابن 3 (قاسم كريم)", "en", "03"),
            new FichierFamilleInfo("fille_1.png", "Acte de naissance — Fille 1 (KACEM Amina)", "شهادة ميلاد — البنت 1 (قاسم أمينة)", "en", "03"),
            new FichierFamilleInfo("fille_2.png", "Acte de naissance — Fille 2 (KACEM Sara)", "شهادة ميلاد — البنت 2 (قاسم سارة)", "en", "03"),
            new FichierFamilleInfo("pere.png", "Acte de naissance — Père (KACEM Abdelkader)", "شهادة ميلاد — الأب (قاسم عبد القادر)", "en", "04"),
            new FichierFamilleInfo("mere.png", "Acte de naissance — Mère (MANSOURI Zohra)", "شهادة ميلاد — الأم (بوزيد فاطمة)", "en", "04"),
            new FichierFamilleInfo("frere.png", "Acte de naissance — Frère (KACEM Mustapha)", "شهادة ميلاد — الأخ (قاسم مصطفى)", "en", "05"),
            new FichierFamilleInfo("soeur.png", "Acte de naissance — Sœur (KACEM Houria)", "شهادة ميلاد — الأخت (قاسم حورية)", "en", "05"),
            new FichierFamilleInfo("temoin_1.png", "Acte de naissance — Témoin 1 (MOKRANI Rachid)", "شهادة ميلاد — الشاهد 1 (مقراني رشيد)", "en", "00"),
            new FichierFamilleInfo("temoin_2.png", "Acte de naissance — Témoin 2 (BELKACEM Nadir)", "شهادة ميلاد — الشاهد 2 (بلقاسم نذير)", "en", "00")
    );

    private final Set<String> hashesAutorises = new HashSet<>();
    private final Map<String, byte[]> contenuParNom = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        for (FichierFamilleInfo info : CATALOGUE) {
            ClassPathResource resource = new ClassPathResource(BASE_PATH + info.getNom());
            if (!resource.exists()) {
                log.warn("Fichier spécimen introuvable dans le classpath : {}{}", BASE_PATH, info.getNom());
                continue;
            }
            try (InputStream in = resource.getInputStream()) {
                byte[] bytes = in.readAllBytes();
                contenuParNom.put(info.getNom(), bytes);
                hashesAutorises.add(calculerSha256(bytes));
            } catch (IOException e) {
                log.error("Impossible de charger le fichier spécimen {}", info.getNom(), e);
            }
        }
        log.info("Dossier démo 'famille' initialisé avec {} actes fictifs autorisés.", hashesAutorises.size());
    }

    public List<FichierFamilleInfo> listerFichiers() {
        return CATALOGUE;
    }

    public byte[] lireFichier(String nomFichier) {
        if (nomFichier == null || nomFichier.contains("..") || nomFichier.contains("/") || nomFichier.contains("\\")) {
            return null;
        }
        return contenuParNom.get(nomFichier);
    }

    public boolean estFichierAutorise(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        try {
            return estContenuAutorise(file.getBytes());
        } catch (IOException e) {
            return false;
        }
    }

    public boolean estContenuAutorise(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return false;
        }
        return hashesAutorises.contains(calculerSha256(bytes));
    }

    private String calculerSha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 non disponible", e);
        }
    }
}
