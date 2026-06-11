package com.muhend.backendai.scheduler;

import com.muhend.backendai.entities.FridaEntity;
import com.muhend.backendai.repository.FridaRepo;
import com.muhend.backendai.service.pipeline.DossierProcessingService;
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
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchJobScheduler {

    private final DossierProcessingService dossierProcessingService;
    private final FridaRepo fridaRepo;

    @Value("${ROOT_PATH:/app/uploads}")
    private String rootPathString;

    @Scheduled(fixedDelay = 3600000)
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

        if (Files.exists(processedFile)) {
            return;
        }

        try {
            Instant lastModifiedTime = Files.getLastModifiedTime(folderPath).toInstant();
            Instant fifteenMinutesAgo = Instant.now().minus(15, ChronoUnit.MINUTES);

            if (lastModifiedTime.isAfter(fifteenMinutesAgo)) {
                log.info("Le dossier {} est trop récent, on attend qu'il soit inactif.", folderPath.getFileName());
                return;
            }

            log.info("Dossier en attente trouvé (Batch) : {}", folderPath);

            // Lancer l'OCR approfondi — retourne la Frida créée
            FridaEntity frida = dossierProcessingService.traiterExtraitsNaissance(folderPath.toString(), "approfondi");

            // Marquer le dossier EN_ATTENTE_REVISION pour que l'utilisateur le révise
            if (frida != null) {
                frida.setStatut(FridaEntity.STATUT_EN_ATTENTE);
                fridaRepo.save(frida);
                log.info("Dossier {} marqué EN_ATTENTE_REVISION.", frida.getNumFrida());
            }

            log.info("Traitement Batch terminé pour le dossier : {}", folderPath);

        } catch (IOException e) {
            log.error("Impossible de lire les propriétés du dossier {}", folderPath, e);
        } catch (Exception e) {
            log.error("Erreur lors du traitement batch du dossier {}", folderPath, e);
        }
    }
}

