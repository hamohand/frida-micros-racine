package com.muhend.backendai.scheduler;

import com.muhend.backendai.service.aibd.EcrireBdService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchJobScheduler {

    private final EcrireBdService ecrireBdService;

    @Value("${ROOT_PATH}")
    private String rootPathString;

    // Cette tâche tourne toutes les heures en fond pour dépiler les dossiers en attente.
    // Vous pouvez la changer pour "0 0 2 * * ?" pour forcer l'exécution seulement à 2h du matin.
    @Scheduled(fixedDelay = 3600000) // S'exécute toutes les heures (3 600 000 ms)
    public void processPendingBatches() {
        log.info("Démarrage du BatchJobScheduler pour rechercher des dossiers en attente...");
        
        Path rootPath = Paths.get(rootPathString);
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            log.warn("Le dossier racine ROOT_PATH n'existe pas : {}", rootPathString);
            return;
        }

        try (Stream<Path> stream = Files.list(rootPath)) {
            stream.filter(Files::isDirectory)
                  .forEach(this::checkAndProcessFolder);
        } catch (IOException e) {
            log.error("Erreur lors de la lecture du dossier racine : {}", e.getMessage(), e);
        }
        
        log.info("Fin de la recherche de dossiers par le BatchJobScheduler.");
    }

    private void checkAndProcessFolder(Path folderPath) {
        Path processedFile = folderPath.resolve(".processed");
        
        // Si le fichier .processed existe, on ignore ce dossier.
        if (Files.exists(processedFile)) {
            return;
        }

        try {
            // Vérifier si le dossier est récent (ex: modifié il y a moins de 15 minutes).
            // On veut éviter de traiter un dossier pendant que l'utilisateur est encore en train d'uploader.
            Instant lastModifiedTime = Files.getLastModifiedTime(folderPath).toInstant();
            Instant fifteenMinutesAgo = Instant.now().minus(15, ChronoUnit.MINUTES);

            if (lastModifiedTime.isAfter(fifteenMinutesAgo)) {
                log.info("Le dossier {} est trop récent, on attend qu'il soit inactif.", folderPath.getFileName());
                return;
            }

            log.info("Dossier en attente trouvé (Batch) : {}", folderPath);
            
            // Lancer le traitement approfondi (qui va créer le fichier .processed à la fin).
            ecrireBdService.traiterExtraitsNaissance(folderPath.toString(), "approfondi");
            
            log.info("Traitement Batch terminé pour le dossier : {}", folderPath);

        } catch (IOException e) {
            log.error("Impossible de lire les propriétés du dossier {}", folderPath, e);
        } catch (Exception e) {
            log.error("Erreur lors du traitement batch du dossier {}", folderPath, e);
        }
    }
}
