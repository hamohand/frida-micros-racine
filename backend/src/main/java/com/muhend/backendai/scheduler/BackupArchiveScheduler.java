package com.muhend.backendai.scheduler;

import com.muhend.backendai.service.ArchiveService;
import com.muhend.backendai.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class BackupArchiveScheduler {

    private final BackupService backupService;
    private final ArchiveService archiveService;

    @Value("${app.backup.auto-enabled:false}")
    private boolean autoBackupEnabled;

    @Value("${app.backup.retention-days:7}")
    private int retentionDays;

    /**
     * Sauvegarde automatique de la base de données active.
     * Par défaut: tous les jours à 2h du matin.
     */
    @Scheduled(cron = "${app.backup.cron:0 0 2 * * *}")
    public void scheduledBackup() {
        if (!autoBackupEnabled) {
            log.debug("Sauvegarde automatique désactivée.");
            return;
        }

        log.info("=== Début sauvegarde automatique quotidienne ===");
        try {
            backupService.createBackup();
            log.info("Sauvegarde automatique réussie.");
        } catch (Exception e) {
            log.error("Échec de la sauvegarde automatique", e);
        }

        // Nettoyage des anciennes sauvegardes
        try {
            int deleted = backupService.cleanupOldBackups(retentionDays);
            if (deleted > 0) {
                log.info("{} ancienne(s) sauvegarde(s) nettoyée(s) (> {} jours)", deleted, retentionDays);
            }
        } catch (Exception e) {
            log.error("Échec du nettoyage des anciennes sauvegardes", e);
        }
        log.info("=== Fin sauvegarde automatique ===");
    }

    /**
     * Archivage automatique des dossiers anciens.
     * Par défaut: le 1er de chaque mois à 3h du matin.
     */
    @Scheduled(cron = "${app.archive.cron:0 0 3 1 * *}")
    public void scheduledArchive() {
        if (!autoBackupEnabled) {
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
