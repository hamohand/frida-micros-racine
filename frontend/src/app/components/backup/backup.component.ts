import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Observable } from 'rxjs';
import {
  BackupService,
  BackupInfo,
  ArchiveInfo,
  FridaArchivable,
  LienTelechargement
} from '../../services/backup.service';
import { AuthService } from '../../services/auth.service';

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

  /** Dernière restauration réussie : son message reste affiché avec un bouton pour l'annuler. */
  derniereRestauration: { message: string; securite: string; dateSecurite: string } | null = null;

  constructor(private backupService: BackupService, private authService: AuthService) {}

  /** Restaurer, supprimer, télécharger et archiver sont réservés au compte Maître (contrôlé aussi côté serveur). */
  get estMaitre(): boolean {
    return this.authService.isMaitre();
  }

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
      error: (err) => { this.showMessage(this.messageErreur(err, 'Erreur lors du chargement des sauvegardes.'), true); this.loading = false; }
    });
  }

  createBackup(): void {
    this.loading = true;
    this.showMessage('Création de la sauvegarde en cours (base de données et documents)...', false);
    this.backupService.createBackup().subscribe({
      next: (backup) => { this.showMessage('Sauvegarde créée : ' + backup.fileName, false); this.loadBackups(); },
      error: (err) => { this.showMessage(this.messageErreur(err, 'Erreur lors de la création.'), true); this.loading = false; }
    });
  }

  restoreBackup(backup: BackupInfo): void {
    const date = this.formatDate(backup.createdAt);
    if (confirm('⚠️ Revenir à l\'état du ' + date + ' ?\n\n'
      + 'La base de données et les documents redeviendront ceux de cette sauvegarde : '
      + 'les dossiers créés ou modifiés depuis seront retirés.\n\n'
      + 'L\'état actuel est d\'abord mis de côté : vous pourrez annuler la restauration juste après.')) {
      this.restaurer(backup.fileName);
    }
  }

  /** Annule la dernière restauration en restaurant l'état mis de côté juste avant. */
  annulerRestauration(): void {
    const annulation = this.derniereRestauration;
    if (!annulation) return;
    if (confirm('Annuler la restauration ?\n\n'
      + 'FRIDA reviendra à l\'état du ' + annulation.dateSecurite + ', juste avant la restauration.\n\n'
      + 'L\'état actuel sera à son tour mis de côté : vous pourrez encore changer d\'avis.')) {
      this.restaurer(annulation.securite);
    }
  }

  private restaurer(fileName: string): void {
    this.loading = true;
    this.derniereRestauration = null;
    this.showMessage('Mise de côté de l\'état actuel, puis restauration en cours...', false);
    this.backupService.restoreBackup(fileName).subscribe({
      next: (res) => {
        this.message = '';
        // Message durable, avec le bouton d'annulation
        this.derniereRestauration = {
          message: res.message || 'Restauration réussie.',
          securite: res.sauvegardeDeSecurite,
          dateSecurite: res.dateSecurite
        };
        this.loadBackups();
      },
      error: (err) => { this.showMessage(this.messageErreur(err, 'Erreur lors de la restauration.'), true); this.loading = false; }
    });
  }

  deleteBackup(fileName: string): void {
    if (confirm('Supprimer la sauvegarde "' + fileName + '" ?')) {
      this.loading = true;
      this.backupService.deleteBackup(fileName).subscribe({
        next: () => { this.showMessage('Sauvegarde supprimée.', false); this.loadBackups(); },
        error: (err) => { this.showMessage(this.messageErreur(err, 'Erreur lors de la suppression.'), true); this.loading = false; }
      });
    }
  }

  downloadBackup(fileName: string): void {
    this.telecharger(this.backupService.lienTelechargementSauvegarde(fileName));
  }

  // ===== ARCHIVES =====
  loadArchives(): void {
    this.loading = true;
    this.backupService.listArchives().subscribe({
      next: (data) => { this.archives = data; this.loading = false; },
      error: (err) => { this.showMessage(this.messageErreur(err, 'Erreur lors du chargement des archives.'), true); this.loading = false; }
    });
  }

  loadArchivableFridas(): void {
    this.showArchivableList = !this.showArchivableList;
    if (this.showArchivableList) {
      this.loading = true;
      this.backupService.getArchivableFridas().subscribe({
        next: (data) => { this.archivableFridas = data; this.loading = false; },
        error: (err) => { this.showMessage(this.messageErreur(err, 'Erreur lors du chargement des dossiers archivables.'), true); this.loading = false; }
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
        error: (err) => { this.showMessage(this.messageErreur(err, 'Erreur lors de l\'archivage.'), true); this.loading = false; }
      });
    }
  }

  autoArchive(): void {
    if (confirm('Archiver maintenant tous les dossiers éligibles ?\n\nTous les dossiers de plus de 6 mois seront retirés de la base active et conservés dans des fichiers archive.')) {
      this.loading = true;
      this.showMessage('Archivage en cours...', false);
      this.backupService.autoArchive().subscribe({
        next: (res) => { this.showMessage(res.message, false); this.loadArchives(); },
        error: (err) => { this.showMessage(this.messageErreur(err, 'Erreur lors de l\'archivage.'), true); this.loading = false; }
      });
    }
  }

  restoreArchive(fileName: string): void {
    if (confirm('Restaurer cette archive dans la base active ?\n\nLe dossier sera de nouveau accessible dans l\'application.')) {
      this.loading = true;
      this.showMessage('Restauration en cours...', false);
      this.backupService.restoreArchive(fileName).subscribe({
        next: (res) => { this.showMessage(res.message || 'Archive restaurée.', false); this.loading = false; },
        error: (err) => { this.showMessage(this.messageErreur(err, 'Erreur lors de la restauration.'), true); this.loading = false; }
      });
    }
  }

  deleteArchive(fileName: string): void {
    if (confirm('⚠️ Supprimer définitivement cette archive ?\n\nCette action est irréversible.')) {
      this.loading = true;
      this.backupService.deleteArchive(fileName).subscribe({
        next: () => { this.showMessage('Archive supprimée.', false); this.loadArchives(); },
        error: (err) => { this.showMessage(this.messageErreur(err, 'Erreur lors de la suppression.'), true); this.loading = false; }
      });
    }
  }

  downloadArchive(fileName: string): void {
    this.telecharger(this.backupService.lienTelechargementArchive(fileName));
  }

  // ===== UTILITAIRES =====

  /**
   * Obtient un lien à usage unique puis le fait suivre par le navigateur : le fichier, qui peut
   * peser plusieurs Go avec les documents, est téléchargé en flux, sans passer par la mémoire de la page.
   */
  private telecharger(lien: Observable<LienTelechargement>): void {
    lien.subscribe({
      next: ({ url }) => {
        const a = document.createElement('a');
        a.href = url;
        document.body.appendChild(a);
        a.click();
        a.remove();
      },
      error: (err) => this.showMessage(this.messageErreur(err, 'Erreur lors du téléchargement.'), true)
    });
  }

  private messageErreur(err: any, defaut: string): string {
    return err?.error?.message || defaut;
  }

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
