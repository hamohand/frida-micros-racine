import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from "@angular/common/http";
import { catchError, map, Observable, throwError } from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class FridaService {
  private apiUrl: string = 'http://localhost:8080'; // initialisation

  constructor(private monHttp: HttpClient) { }

  // Méthode de configuration de l'URL (passe l'API URL ici)
  setApiUrl(apiUrl: string): void {
    this.apiUrl = apiUrl;
  }

  lancerApi(apiUrl: string): Observable<any> {
    this.setApiUrl(apiUrl);
    const headers = new HttpHeaders({ 'Content-Type': 'application/x-www-form-urlencoded' });

    return this.monHttp.get<any>(this.apiUrl, { headers: headers }).pipe(
      // Traitement des données de réponse
      map((data) => {
        if (!data || typeof data !== 'object') {
          throw new Error('frida.service : Réponse non valide');
        }
        return data;
      }),
      // Gestion d'erreur centralisée
      catchError((error) => {
        console.error('frida.service : Erreur API détectée:', error);
        return throwError(() => new Error('frida.service : Une erreur est survenue en appelant l’API.'));
      })
    );
  }
  //
  getFridaList(): Observable<{ numFrida: string; dateCreation: string; dateNaissane: string; nom: string }[]> {
    const url = `${this.apiUrl}/api/frida/fridas`; // Remplacez par l'URL correcte de votre API
    return this.monHttp.get<{ numFrida: string; dateCreation: string; dateNaissane: string; nom: string }[]>(url).pipe(
      catchError((error) => {
        console.error('Erreur lors de la récupération des Frida:', error);
        return throwError(() => new Error('Une erreur est survenue en chargeant les Frida.'));
      })
    );
  }
}
