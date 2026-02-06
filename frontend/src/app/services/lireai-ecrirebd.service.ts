import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
    providedIn: 'root',
})
export class LireaiEcrirebdService {
    private apiUrl = 'http://localhost:8080/api/pdfs'; // Changez ce chemin si nécessaire

    constructor(private http: HttpClient) {}

    // Nouvelle méthode pour appeler le contrôleur `lireAiEcrireBd`
    lireAiEcrireBd(): Observable<any> {
        return this.http.get(`${this.apiUrl}/lireai-ecrirebd`);
    }
}