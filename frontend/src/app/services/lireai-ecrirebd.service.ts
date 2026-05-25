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

    clearLatestFolder(): Observable<any> {
        // Point to FolderController instead of OcrProcessingController
        const folderApiUrl = this.apiUrl.replace('/pdfs', '/folders');
        return this.http.delete(`${folderApiUrl}/clear-latest`);
    }

    sauvegarderFiche(numFrida: string, ficheData: any): Observable<any> {
        return this.http.post(`${this.apiUrl}/sauvegarder-fiche/${numFrida}`, ficheData);
    }

    lancerCalcul(numFrida: string): Observable<any> {
        return this.http.post(`${this.apiUrl}/lancer-calcul/${numFrida}`, {});
    }

    getChampsSuspects(numFrida: string): Observable<any[]> {
        return this.http.get<any[]>(`/api/frida/champs-suspects/${numFrida}`);
    }

    appliquerCorrections(numFrida: string, corrections: {personneId: string|null, champ: string, valeur: string}[]): Observable<any> {
        return this.http.post(`/api/frida/appliquer-corrections/${numFrida}`, corrections);
    }

    mettreEnAttenteOcr(numFrida: string, corrections: {personneId: string|null, champ: string, valeur: string}[]): Observable<any> {
        return this.http.post(`/api/frida/mettre-en-attente/${numFrida}`, corrections);
    }
}