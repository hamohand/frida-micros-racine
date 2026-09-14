package com.muhend.backendai.scheduler;

import com.muhend.backendai.dto.BackupInfo;
import com.muhend.backendai.service.ArchiveService;
import com.muhend.backendai.service.BackupService;
import com.muhend.backendai.service.ParametreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Profile("!calc-only")
@Component
@Slf4j
@RequiredArgsConstructor
public class BackupArchiveScheduler {

    private static final DateTimeFormatter ANNEE_MOIS = DateTimeFormatter.ofPattern("yyyy-MM");

    private final BackupService backupService;
    private final ArchiveService archiveService;
    private final ParametreService parametreService;

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
     * Archivage automatique des dossiers de plus de {@code app.archive.threshold-months}, le
     * premier jour ouvré de chaque mois (lundi-vendredi ; les jours fériés ne sont pas exclus).
     * Vérifié comme la sauvegarde — peu après le démarrage puis toutes les heures, pas à une heure
     * fixe — pour le même motif : un poste éteint la nuit et le week-end n'atteindrait jamais un
     * cron fixé au 1er du mois à 3h. Le mois déjà traité est retenu dans la table {@code parametres}
     * (survit à un redémarrage) ; un échec ne l'enregistre pas, pour retenter à la vérification
     * suivante.
     */
    @Scheduled(initialDelayString = "${app.archive.auto-initial-delay-ms:180000}",
            fixedDelayString = "${app.archive.auto-check-interval-ms:3600000}")
    public void verifierArchivageAutomatique() {
        if (!autoArchiveEnabled) {
            return;
        }
        LocalDate aujourdhui = aujourdhui();
        DayOfWeek jour = aujourdhui.getDayOfWeek();
        if (jour == DayOfWeek.SATURDAY || jour == DayOfWeek.SUNDAY) {
            log.debug("Archivage automatique : {} n'est pas un jour ouvré, on retente au prochain.", jour);
            return;
        }
        String moisCourant = aujourdhui.format(ANNEE_MOIS);
        if (parametreService.getDernierArchivageAuto().map(moisCourant::equals).orElse(false)) {
            log.debug("Archivage automatique déjà effectué pour {}.", moisCourant);
            return;
        }
        log.info("=== Archivage automatique du mois {} (premier jour ouvré) ===", moisCourant);
        try {
            int count = archiveService.autoArchive();
            parametreService.setDernierArchivageAuto(moisCourant);
            log.info("Archivage automatique terminé : {} dossier(s) archivé(s)", count);
        } catch (Exception e) {
            log.error("Échec de l'archivage automatique, nouvelle tentative à la prochaine vérification", e);
        }
    }

    /** Point d'extension pour les tests (BackupArchiveSchedulerTest fixe un jour donné). */
    protected LocalDate aujourdhui() {
        return LocalDate.now();
    }
}
