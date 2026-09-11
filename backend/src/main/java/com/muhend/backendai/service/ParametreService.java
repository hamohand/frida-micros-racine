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
}
