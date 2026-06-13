import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

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

export interface Fraction {
  numerateur: number;
  denominateur: number;
}

export interface HeritierPart {
  heritier: string;
  baseCalcul?: string;
  cadreLegal?: string;
  part?: Fraction;
  partIrreductible?: Fraction;
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
  private apiUrl = '/api/calculs/simuler';

  constructor(private http: HttpClient) {}

  simulerCalcul(request: FamilyRequest): Observable<HeritageResponse> {
    return this.http.post<HeritageResponse>(this.apiUrl, request);
  }
}
