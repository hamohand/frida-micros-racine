import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Parametres {
  verificationPhonetique: boolean;
  /** IP ou nom d'hôte du poste sur le réseau local (sans schéma ni port), vide si non configurée. */
  adresseReseauLocale: string;
}

@Injectable({
  providedIn: 'root'
})
export class ParametresService {
  private url = '/api/parametres';

  constructor(private http: HttpClient) {}

  lire(): Observable<Parametres> {
    return this.http.get<Parametres>(this.url);
  }

  /** Un seul champ à la fois suffit : les autres ne sont pas modifiés. */
  modifier(parametres: Partial<Parametres>): Observable<Parametres> {
    return this.http.put<Parametres>(this.url, parametres);
  }
}
