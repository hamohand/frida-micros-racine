import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface BackupInfo {
  fileName: string;
  sizeBytes: number;
  createdAt: string;
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
    return this.http.post(`${this.backupUrl}/${fileName}/restore`, {});
  }

  deleteBackup(fileName: string): Observable<any> {
    return this.http.delete(`${this.backupUrl}/${fileName}`);
  }

  getDownloadUrl(fileName: string): string {
    return `${this.backupUrl}/${fileName}/download`;
  }

  // ===== ARCHIVES =====
  listArchives(): Observable<ArchiveInfo[]> {
    return this.http.get<ArchiveInfo[]>(this.archiveUrl);
  }

  getArchivableFridas(): Observable<FridaArchivable[]> {
    return this.http.get<FridaArchivable[]>(`${this.archiveUrl}/archivable`);
  }

  archiveFrida(numFrida: string): Observable<ArchiveInfo> {
    return this.http.post<ArchiveInfo>(`${this.archiveUrl}/${numFrida}`, {});
  }

  restoreArchive(fileName: string): Observable<any> {
    return this.http.post(`${this.archiveUrl}/${fileName}/restore`, {});
  }

  deleteArchive(fileName: string): Observable<any> {
    return this.http.delete(`${this.archiveUrl}/${fileName}`);
  }

  getArchiveDownloadUrl(fileName: string): string {
    return `${this.archiveUrl}/${fileName}/download`;
  }

  autoArchive(): Observable<any> {
    return this.http.post(`${this.archiveUrl}/auto`, {});
  }
}
