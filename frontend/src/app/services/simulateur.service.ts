import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface FamilyRequest {
  sexeDefunt: string;
  nbConjoints: number;
  pereVivant: boolean;
  mereVivante: boolean;
  grandPerePaternelVivant: boolean;
  grandMerePaternelleVivante: boolean;
  nbFilles: number;
  nbGarcons: number;
  nbSoeurs: number;
  nbFreres: number;
  nbOncles: number;
  nbCousins: number;
  nbPetitsFils: number;
  nbPetitesFilles: number;
  sexeParentPredecede: string;
}

export interface HeritierPart {
  heritier: string;
  numerateur: number;
  denominateur: number;
  pourcentage: number;
}

export interface HeritageResponse {
  denominateurCommun: number;
  heritiers: HeritierPart[];
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class SimulateurService {
  private apiUrl = `${environment.apiUrl}/calculs/simuler`;

  constructor(private http: HttpClient) {}

  simulerCalcul(request: FamilyRequest): Observable<HeritageResponse> {
    return this.http.post<HeritageResponse>(this.apiUrl, request);
  }
}
