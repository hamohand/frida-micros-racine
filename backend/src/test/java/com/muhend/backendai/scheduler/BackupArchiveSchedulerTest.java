package com.muhend.backendai.scheduler;

import com.muhend.backendai.dto.BackupInfo;
import com.muhend.backendai.service.ArchiveService;
import com.muhend.backendai.service.BackupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Sauvegarde automatique « si la dernière a plus de 24 h », et archivage automatique
 * séparé, désactivé par défaut.
 */
@ExtendWith(MockitoExtension.class)
class BackupArchiveSchedulerTest {

    @Mock
    private BackupService backupService;

    @Mock
    private ArchiveService archiveService;

    private BackupArchiveScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new BackupArchiveScheduler(backupService, archiveService);
        ReflectionTestUtils.setField(scheduler, "autoBackupEnabled", true);
        ReflectionTestUtils.setField(scheduler, "intervalleHeures", 24L);
        ReflectionTestUtils.setField(scheduler, "aConserver", 7);
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

    @Test
    void archivageAutomatique_DesactiveParDefaut() {
        scheduler.scheduledArchive();

        verifyNoInteractions(archiveService);
    }
}
