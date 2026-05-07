import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
    providedIn: 'root',
})
export class LireaiEcrirebdService {
    private apiUrl = '/api/pdfs'; // Changez ce chemin si nécessaire

    constructor(private http: HttpClient) {}

    // Nouvelle méthode pour appeler le contrôleur `lireAiEcrireBd` avec paramètre de mode
    lireAiEcrireBd(mode: string = 'rapide'): Observable<any> {
        return this.http.get(`${this.apiUrl}/lireai-ecrirebd?mode=${mode}`);
    }
}