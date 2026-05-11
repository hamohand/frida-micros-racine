import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  BackupService,
  BackupInfo,
  ArchiveInfo,
  FridaArchivable
} from '../../services/backup.service';

@Component({
  selector: 'app-backup',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './backup.component.html',
  styleUrls: ['./backup.component.css']
})
export class BackupComponent implements OnInit {
  activeTab: 'backups' | 'archives' = 'backups';

  // Sauvegardes
  backups: BackupInfo[] = [];

  // Archives
  archives: ArchiveInfo[] = [];
  archivableFridas: FridaArchivable[] = [];
  showArchivableList = false;

  loading = false;
  message = '';
  isError = false;

  constructor(private backupService: BackupService) {}

  ngOnInit(): void {
    this.loadBackups();
  }

  switchTab(tab: 'backups' | 'archives'): void {
    this.activeTab = tab;
    this.message = '';
    if (tab === 'backups') {
      this.loadBackups();
    } else {
      this.loadArchives();
    }
  }

  // ===== SAUVEGARDES =====
  loadBackups(): void {
    this.loading = true;
    this.backupService.listBackups().subscribe({
      next: (data) => { this.backups = data; this.loading = false; },
      error: () => { this.showMessage('Erreur lors du chargement des sauvegardes.', true); this.loading = false; }
    });
  }

  createBackup(): void {
    this.loading = true;
    this.showMessage('Création de la sauvegarde en cours...', false);
    this.backupService.createBackup().subscribe({
      next: (backup) => { this.showMessage('Sauvegarde créée : ' + backup.fileName, false); this.loadBackups(); },
      error: () => { this.showMessage('Erreur lors de la création.', true); this.loading = false; }
    });
  }

  restoreBackup(fileName: string): void {
    if (confirm('⚠️ Restaurer la sauvegarde "' + fileName + '" ?\nToutes les données actuelles seront remplacées par celles de cette sauvegarde.')) {
      this.loading = true;
      this.showMessage('Restauration en cours...', false);
      this.backupService.restoreBackup(fileName).subscribe({
        next: (res) => { this.showMessage(res.message || 'Restauration réussie.', false); this.loading = false; },
        error: () => { this.showMessage('Erreur lors de la restauration.', true); this.loading = false; }
      });
    }
  }

  deleteBackup(fileName: string): void {
    if (confirm('Supprimer la sauvegarde "' + fileName + '" ?')) {
      this.loading = true;
      this.backupService.deleteBackup(fileName).subscribe({
        next: () => { this.showMessage('Sauvegarde supprimée.', false); this.loadBackups(); },
        error: () => { this.showMessage('Erreur lors de la suppression.', true); this.loading = false; }
      });
    }
  }

  downloadBackupUrl(fileName: string): string {
    return this.backupService.getDownloadUrl(fileName);
  }

  // ===== ARCHIVES =====
  loadArchives(): void {
    this.loading = true;
    this.backupService.listArchives().subscribe({
      next: (data) => { this.archives = data; this.loading = false; },
      error: () => { this.showMessage('Erreur lors du chargement des archives.', true); this.loading = false; }
    });
  }

  loadArchivableFridas(): void {
    this.showArchivableList = !this.showArchivableList;
    if (this.showArchivableList) {
      this.loading = true;
      this.backupService.getArchivableFridas().subscribe({
        next: (data) => { this.archivableFridas = data; this.loading = false; },
        error: () => { this.showMessage('Erreur lors du chargement des dossiers archivables.', true); this.loading = false; }
      });
    }
  }

  archiveFrida(numFrida: string, nom: string): void {
    if (confirm('Archiver le dossier "' + numFrida + ' — ' + nom + '" ?\n\nCe dossier sera retiré de la base active et conservé dans un fichier archive.')) {
      this.loading = true;
      this.showMessage('Archivage en cours...', false);
      this.backupService.archiveFrida(numFrida).subscribe({
        next: (info) => {
          this.showMessage('Dossier ' + info.numFrida + ' archivé avec succès.', false);
          this.loadArchivableFridas();
          this.loadArchives();
        },
        error: () => { this.showMessage('Erreur lors de l\'archivage.', true); this.loading = false; }
      });
    }
  }

  autoArchive(): void {
    if (confirm('Lancer l\'archivage automatique ?\n\nTous les dossiers de plus de 6 mois seront archivés.')) {
      this.loading = true;
      this.showMessage('Archivage automatique en cours...', false);
      this.backupService.autoArchive().subscribe({
        next: (res) => { this.showMessage(res.message, false); this.loadArchives(); },
        error: () => { this.showMessage('Erreur lors de l\'archivage automatique.', true); this.loading = false; }
      });
    }
  }

  restoreArchive(fileName: string): void {
    if (confirm('Restaurer cette archive dans la base active ?\n\nLe dossier sera de nouveau accessible dans l\'application.')) {
      this.loading = true;
      this.showMessage('Restauration en cours...', false);
      this.backupService.restoreArchive(fileName).subscribe({
        next: (res) => { this.showMessage(res.message || 'Archive restaurée.', false); this.loading = false; },
        error: (err) => {
          const msg = err?.error?.message || 'Erreur lors de la restauration.';
          this.showMessage(msg, true);
          this.loading = false;
        }
      });
    }
  }

  deleteArchive(fileName: string): void {
    if (confirm('⚠️ Supprimer définitivement cette archive ?\n\nCette action est irréversible.')) {
      this.loading = true;
      this.backupService.deleteArchive(fileName).subscribe({
        next: () => { this.showMessage('Archive supprimée.', false); this.loadArchives(); },
        error: () => { this.showMessage('Erreur lors de la suppression.', true); this.loading = false; }
      });
    }
  }

  downloadArchiveUrl(fileName: string): string {
    return this.backupService.getArchiveDownloadUrl(fileName);
  }

  // ===== UTILITAIRES =====
  formatBytes(bytes: number, decimals = 2): string {
    if (!+bytes) return '0 o';
    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['o', 'Ko', 'Mo', 'Go', 'To'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(dm))} ${sizes[i]}`;
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleString('fr-FR');
  }

  private showMessage(msg: string, isError: boolean): void {
    this.message = msg;
    this.isError = isError;
    if (!isError) {
      setTimeout(() => { this.message = ''; }, 8000);
    }
  }
}
