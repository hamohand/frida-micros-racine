import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface BackupInfo {
  fileName: string;
  sizeBytes: number;
  createdAt: string;
  automatique: boolean;
  /** État mis de côté juste avant une restauration : la restaurer annule celle-ci. */
  avantRestauration: boolean;
  documentsInclus: boolean;
}

export interface ArchiveInfo {
  fileName: string;
  numFrida: string;
  nomDefunt: string;
  prenomDefunt: string;
  dateCreationFrida: string;
  dateArchivage: string;
  sizeBytes: number;
  includesFiles: boolean;
}

export interface FridaArchivable {
  numFrida: string;
  dateCreation: string;
  dateNaissance: string;
  nom: string;
  prenom: string;
  requiresCorrection: boolean;
}

/** Lien à usage unique (60 s) : le navigateur télécharge ensuite le fichier en flux. */
export interface LienTelechargement {
  url: string;
}

@Injectable({
  providedIn: 'root'
})
export class BackupService {
  private backupUrl = '/api/backups';
  private archiveUrl = '/api/archives';

  constructor(private http: HttpClient) {}

  // ===== SAUVEGARDES =====
  listBackups(): Observable<BackupInfo[]> {
    return this.http.get<BackupInfo[]>(this.backupUrl);
  }

  createBackup(): Observable<BackupInfo> {
    return this.http.post<BackupInfo>(this.backupUrl, {});
  }

  restoreBackup(fileName: string): Observable<any> {
    return this.http.post(`${this.backupUrl}/${encodeURIComponent(fileName)}/restore`, {});
  }

  deleteBackup(fileName: string): Observable<any> {
    return this.http.delete(`${this.backupUrl}/${encodeURIComponent(fileName)}`);
  }

  lienTelechargementSauvegarde(fileName: string): Observable<LienTelechargement> {
    return this.http.post<LienTelechargement>(`${this.backupUrl}/${encodeURIComponent(fileName)}/lien-telechargement`, {});
  }

  // ===== ARCHIVES =====
  listArchives(): Observable<ArchiveInfo[]> {
    return this.http.get<ArchiveInfo[]>(this.archiveUrl);
  }

  getArchivableFridas(): Observable<FridaArchivable[]> {
    return this.http.get<FridaArchivable[]>(`${this.archiveUrl}/archivable`);
  }

  archiveFrida(numFrida: string): Observable<ArchiveInfo> {
    return this.http.post<ArchiveInfo>(`${this.archiveUrl}/${encodeURIComponent(numFrida)}`, {});
  }

  restoreArchive(fileName: string): Observable<any> {
    return this.http.post(`${this.archiveUrl}/${encodeURIComponent(fileName)}/restore`, {});
  }

  deleteArchive(fileName: string): Observable<any> {
    return this.http.delete(`${this.archiveUrl}/${encodeURIComponent(fileName)}`);
  }

  lienTelechargementArchive(fileName: string): Observable<LienTelechargement> {
    return this.http.post<LienTelechargement>(`${this.archiveUrl}/${encodeURIComponent(fileName)}/lien-telechargement`, {});
  }

  autoArchive(): Observable<any> {
    return this.http.post(`${this.archiveUrl}/auto`, {});
  }
}
