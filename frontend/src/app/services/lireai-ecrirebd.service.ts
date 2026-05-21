import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
    providedIn: 'root',
})
export class LireaiEcrirebdService {
    private apiUrl = '/api/pdfs'; // Changez ce chemin si nécessaire

    constructor(private http: HttpClient) {}

    // Ancienne méthode complète (optionnelle, gardée pour la rétrocompatibilité si besoin)
    lireAiEcrireBd(mode: string = 'rapide'): Observable<any> {
        return this.http.get(`${this.apiUrl}/lireai-ecrirebd?mode=${mode}`);
    }

    analyzeComposition(mode: string = 'rapide'): Observable<any> {
        return this.http.get(`${this.apiUrl}/analyze-composition?mode=${mode}`);
    }

    updateFrida(numFrida: string, mode: string = 'rapide'): Observable<any> {
        return this.http.get(`${this.apiUrl}/update-frida/${numFrida}?mode=${mode}`);
    }
}