import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Parametres {
  verificationPhonetique: boolean;
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

  modifier(parametres: Parametres): Observable<Parametres> {
    return this.http.put<Parametres>(this.url, parametres);
  }
}
