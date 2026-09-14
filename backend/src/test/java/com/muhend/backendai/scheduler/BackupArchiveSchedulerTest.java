package com.muhend.backendai.scheduler;

import com.muhend.backendai.dto.BackupInfo;
import com.muhend.backendai.service.ArchiveService;
import com.muhend.backendai.service.BackupService;
import com.muhend.backendai.service.ParametreService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Sauvegarde automatique « si la dernière a plus de 24 h », et archivage automatique séparé, le
 * premier jour ouvré de chaque mois — désactivé par défaut. La date du jour est fixée via
 * {@link BackupArchiveScheduler#aujourdhui()} : les tests ne dépendent pas du jour réel d'exécution.
 */
@ExtendWith(MockitoExtension.class)
class BackupArchiveSchedulerTest {

    /** Mardi, choisi arbitrairement comme jour ouvré de référence. */
    private static final LocalDate MARDI = LocalDate.of(2026, 9, 15);
    private static final LocalDate SAMEDI = LocalDate.of(2026, 9, 19);
    private static final LocalDate DIMANCHE = LocalDate.of(2026, 9, 20);

    @Mock
    private BackupService backupService;

    @Mock
    private ArchiveService archiveService;

    @Mock
    private ParametreService parametreService;

    private LocalDate jourDuTest = MARDI;

    private BackupArchiveScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new BackupArchiveScheduler(backupService, archiveService, parametreService) {
            @Override
            protected LocalDate aujourdhui() {
                return jourDuTest;
            }
        };
        ReflectionTestUtils.setField(scheduler, "autoBackupEnabled", true);
        ReflectionTestUtils.setField(scheduler, "intervalleHeures", 24L);
        ReflectionTestUtils.setField(scheduler, "aConserver", 7);
        ReflectionTestUtils.setField(scheduler, "autoArchiveEnabled", true);
    }

    @Test
    void desactivee_RienNeSePasse() {
        ReflectionTestUtils.setField(scheduler, "autoBackupEnabled", false);

        scheduler.verifierSauvegardeAutomatique();

        verifyNoInteractions(backupService);
    }

    @Test
    void derniereSauvegardeRecente_PasDeNouvelleSauvegarde() throws Exception {
        when(backupService.derniereSauvegarde()).thenReturn(Optional.of(OffsetDateTime.now().minusHours(3)));

        scheduler.verifierSauvegardeAutomatique();

        verify(backupService, never()).createBackup(anyBoolean());
        verify(backupService, never()).nettoyerSauvegardesAutomatiques(anyInt());
    }

    @Test
    void derniereSauvegardeDePlusDe24h_SauvegardeAutomatiquePuisNettoyage() throws Exception {
        when(backupService.derniereSauvegarde()).thenReturn(Optional.of(OffsetDateTime.now().minusHours(30)));
        when(backupService.createBackup(true)).thenReturn(BackupInfo.builder().fileName("frida_auto_x").build());

        scheduler.verifierSauvegardeAutomatique();

        InOrder ordre = inOrder(backupService);
        ordre.verify(backupService).createBackup(true);
        ordre.verify(backupService).nettoyerSauvegardesAutomatiques(7);
    }

    @Test
    void aucuneSauvegarde_SauvegardeAutomatique() throws Exception {
        when(backupService.derniereSauvegarde()).thenReturn(Optional.empty());
        when(backupService.createBackup(true)).thenReturn(BackupInfo.builder().fileName("frida_auto_x").build());

        scheduler.verifierSauvegardeAutomatique();

        verify(backupService).createBackup(true);
    }

    @Test
    void echecDeLaSauvegarde_AucuneSuppression() throws Exception {
        when(backupService.derniereSauvegarde()).thenReturn(Optional.empty());
        when(backupService.createBackup(true)).thenThrow(new IllegalStateException("pg_dump a échoué"));

        scheduler.verifierSauvegardeAutomatique();

        verify(backupService, never()).nettoyerSauvegardesAutomatiques(anyInt());
    }

    // ===== Archivage automatique =====

    @Test
    void archivage_DesactiveParDefaut_RienNeSePasse() {
        ReflectionTestUtils.setField(scheduler, "autoArchiveEnabled", false);

        scheduler.verifierArchivageAutomatique();

        verifyNoInteractions(archiveService, parametreService);
    }

    @Test
    void archivage_JourOuvreEtPasEncoreFaitCeMois_ArchiveEtEnregistreLeMois() {
        when(parametreService.getDernierArchivageAuto()).thenReturn(Optional.empty());
        when(archiveService.autoArchive()).thenReturn(3);

        scheduler.verifierArchivageAutomatique();

        verify(archiveService).autoArchive();
        verify(parametreService).setDernierArchivageAuto("2026-09");
    }

    @Test
    void archivage_DejaFaitCeMois_RienNeSePasse() {
        when(parametreService.getDernierArchivageAuto()).thenReturn(Optional.of("2026-09"));

        scheduler.verifierArchivageAutomatique();

        verifyNoInteractions(archiveService);
    }

    @Test
    void archivage_MoisDUnAnAuparavant_ConsidereCommeNonFait() {
        // "2025-09" ne doit pas être confondu avec "2026-09"
        when(parametreService.getDernierArchivageAuto()).thenReturn(Optional.of("2025-09"));
        when(archiveService.autoArchive()).thenReturn(0);

        scheduler.verifierArchivageAutomatique();

        verify(archiveService).autoArchive();
    }

    @Test
    void archivage_Samedi_RienNeSePasseMemeSiPasEncoreFaitCeMois() {
        jourDuTest = SAMEDI;

        scheduler.verifierArchivageAutomatique();

        verifyNoInteractions(archiveService, parametreService);
    }

    @Test
    void archivage_Dimanche_RienNeSePasse() {
        jourDuTest = DIMANCHE;

        scheduler.verifierArchivageAutomatique();

        verifyNoInteractions(archiveService, parametreService);
    }

    @Test
    void archivage_Echec_MoisNonEnregistre() {
        when(parametreService.getDernierArchivageAuto()).thenReturn(Optional.empty());
        when(archiveService.autoArchive()).thenThrow(new RuntimeException("panne"));

        scheduler.verifierArchivageAutomatique();

        verify(parametreService, never()).setDernierArchivageAuto(anyString());
    }
}
