package com.muhend.backendai.service.pipeline;

import com.muhend.backendai.service.calculs_outils.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service responsable de la génération d'identifiants uniques pour les fiches Frida.
 * L'identifiant est composé de la date de naissance nettoyée + un timestamp précis.
 */
@Service
public class FridaIdentifierService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Génère un identifiant Frida à partir de la date de naissance du défunt.
     *
     * @param dateNaissance Date de naissance sous forme de chaîne.
     * @return Identifiant unique (dateNaissance nettoyée + timestamp).
     */
    public String genererIdentifiant(String dateNaissance) {
        String base = dateNaissance != null ? dateNaissance : "";
        base = StringUtils.replaceTrimSpaces(base, '.');
        base = StringUtils.replaceAllTrimSpaces(base, '/');
        base = StringUtils.replaceAllTrimSpaces(base, '-');
        return base + genererTimestamp();
    }

    /**
     * Génère un timestamp formaté pour l'identifiant (yyyyMMddHHmmss).
     *
     * @return Le timestamp courant formaté.
     */
    public String genererTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP_FORMAT);
    }
}
