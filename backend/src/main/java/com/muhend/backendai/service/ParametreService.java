package com.muhend.backendai.service;

import com.muhend.backendai.entities.ParametreEntity;
import com.muhend.backendai.repository.ParametreRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Réglages applicatifs enregistrés en base (table parametres).
 */
@Profile("!calc-only")
@Service
@Slf4j
public class ParametreService {

    /** Vérification phonétique nom arabe / nom latin. */
    public static final String VERIFICATION_PHONETIQUE = "ocr.verification-phonetique";

    /** Année-mois (yyyy-MM) du dernier archivage automatique mensuel effectué avec succès. */
    public static final String DERNIER_ARCHIVAGE_AUTO = "archive.dernier-passage-auto";

    /**
     * Adresse (IP ou nom d'hôte, sans schéma ni port) à laquelle le poste répond sur le réseau
     * local du cabinet — saisie par le Maître, trouvée avec « ipconfig ». Sert à construire l'URL
     * que le QR code du scan NFC par smartphone donne au téléphone : celui-ci ne peut pas joindre
     * ce PC via « localhost », qui ne désigne que lui-même une fois sur le réseau Wi-Fi.
     */
    public static final String ADRESSE_RESEAU_LOCALE = "reseau.adresse-locale";

    private final ParametreRepo parametreRepo;

    public ParametreService(ParametreRepo parametreRepo) {
        this.parametreRepo = parametreRepo;
    }

    /**
     * Désactivée par défaut : avec le QR code des extraits de naissance, la lecture du nom est
     * exacte et le contrôle n'apporte rien. Utile si les noms sont lus par OCR classique.
     */
    public boolean isVerificationPhonetiqueActive() {
        return parametreRepo.findById(VERIFICATION_PHONETIQUE)
                .map(p -> Boolean.parseBoolean(p.getValeur()))
                .orElse(false);
    }

    public void setVerificationPhonetiqueActive(boolean active) {
        parametreRepo.save(new ParametreEntity(VERIFICATION_PHONETIQUE, String.valueOf(active)));
        log.info("Vérification phonétique des noms {}", active ? "activée" : "désactivée");
    }

    /** Année-mois (yyyy-MM) du dernier archivage automatique réussi, absent si aucun n'a encore eu lieu. */
    public java.util.Optional<String> getDernierArchivageAuto() {
        return parametreRepo.findById(DERNIER_ARCHIVAGE_AUTO).map(ParametreEntity::getValeur);
    }

    public void setDernierArchivageAuto(String anneeMois) {
        parametreRepo.save(new ParametreEntity(DERNIER_ARCHIVAGE_AUTO, anneeMois));
    }

    /** Vide si le Maître ne l'a pas encore saisie. */
    public String getAdresseReseauLocale() {
        return parametreRepo.findById(ADRESSE_RESEAU_LOCALE).map(ParametreEntity::getValeur).orElse("");
    }

    public void setAdresseReseauLocale(String adresse) {
        parametreRepo.save(new ParametreEntity(ADRESSE_RESEAU_LOCALE, adresse));
        log.info("Adresse réseau locale enregistrée : {}", adresse);
    }
}
