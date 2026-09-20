import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Brouillon {
  id: number;
  folderName: string;
  folderPath: string;
  nomDefunt: string;
  prenomDefunt: string;
  dateCreation: string;
  statut: string;
}

export interface BrouillonFichiers {
  subfolders: { [key: string]: string[] };
  totalFiles: number;
}

@Injectable({ providedIn: 'root' })
export class BrouillonService {
  private apiUrl = '/api';

  constructor(private http: HttpClient) {}

  list(): Observable<Brouillon[]> {
    return this.http.get<Brouillon[]>(`${this.apiUrl}/brouillons`);
  }

  get(id: number): Observable<Brouillon> {
    return this.http.get<Brouillon>(`${this.apiUrl}/brouillons/${id}`);
  }

  getFichiers(id: number): Observable<BrouillonFichiers> {
    return this.http.get<BrouillonFichiers>(`${this.apiUrl}/brouillons/${id}/fichiers`);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/brouillons/${id}`);
  }
}
