package com.muhend.backendai.scheduler;

import com.muhend.backendai.dto.BackupInfo;
import com.muhend.backendai.service.ArchiveService;
import com.muhend.backendai.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;

@Profile("!calc-only")
@Component
@Slf4j
@RequiredArgsConstructor
public class BackupArchiveScheduler {

    private final BackupService backupService;
    private final ArchiveService archiveService;

    @Value("${app.backup.auto-enabled:false}")
    private boolean autoBackupEnabled;

    @Value("${app.backup.auto-interval-hours:24}")
    private long intervalleHeures;

    @Value("${app.backup.auto-keep:7}")
    private int aConserver;

    /** Distinct de la sauvegarde : l'archivage retire des dossiers de la base active. */
    @Value("${app.archive.auto-enabled:false}")
    private boolean autoArchiveEnabled;

    /**
     * Sauvegarde automatique si la dernière (écran, automatique ou sauvegarder.bat) a plus de
     * {@code intervalleHeures}. Vérifiée peu après le démarrage puis toutes les heures : un poste
     * de notaire est souvent éteint la nuit, une heure fixe ne serait presque jamais atteinte.
     */
    @Scheduled(initialDelayString = "${app.backup.auto-initial-delay-ms:120000}",
            fixedDelayString = "${app.backup.auto-check-interval-ms:3600000}")
    public void verifierSauvegardeAutomatique() {
        if (!autoBackupEnabled) {
            return;
        }
        Optional<OffsetDateTime> derniere = backupService.derniereSauvegarde();
        if (derniere.isPresent() && derniere.get().isAfter(OffsetDateTime.now().minusHours(intervalleHeures))) {
            log.debug("Dernière sauvegarde du {} : pas de sauvegarde automatique.", derniere.get());
            return;
        }
        try {
            BackupInfo sauvegarde = backupService.createBackup(true);
            log.info("Sauvegarde automatique créée : {}", sauvegarde.getFileName());
        } catch (Exception e) {
            log.error("Échec de la sauvegarde automatique", e);
            return;
        }
        int supprimees = backupService.nettoyerSauvegardesAutomatiques(aConserver);
        if (supprimees > 0) {
            log.info("{} ancienne(s) sauvegarde(s) automatique(s) supprimée(s), {} conservée(s)", supprimees, aConserver);
        }
    }

    /**
     * Archivage automatique des dossiers anciens.
     * Par défaut: le 1er de chaque mois à 3h du matin, s'il est activé (désactivé par défaut).
     */
    @Scheduled(cron = "${app.archive.cron:0 0 3 1 * *}")
    public void scheduledArchive() {
        if (!autoArchiveEnabled) {
            log.debug("Archivage automatique désactivé.");
            return;
        }

        log.info("=== Début archivage automatique mensuel ===");
        try {
            int count = archiveService.autoArchive();
            log.info("Archivage automatique terminé: {} dossier(s) archivé(s)", count);
        } catch (Exception e) {
            log.error("Échec de l'archivage automatique", e);
        }
        log.info("=== Fin archivage automatique ===");
    }
}
