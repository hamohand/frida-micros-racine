import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface FridaLicense {
  id: string;
  licenseKey: string;
  notaryName: string;
  validUntil: string;
  hardwareId: string;
  active: boolean;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class LicenseService {
  // En dev local, on peut pointer vers localhost:8083, en prod on utilise l'URL API_URL.
  // Pour faire simple, on va utiliser une variable d'environnement ou chemin relatif si servi par nginx.
  // Mais ici on hardcode ou utilise l'environnement.
  private apiUrl = '/api/admin/licenses';

  constructor(private http: HttpClient) {}

  getLicenses(): Observable<FridaLicense[]> {
    return this.http.get<FridaLicense[]>(this.apiUrl);
  }

  createLicense(notaryName: string, validMonths: number): Observable<FridaLicense> {
    return this.http.post<FridaLicense>(this.apiUrl, { notaryName, validMonths });
  }

  revokeLicense(id: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/revoke`, {});
  }
}
