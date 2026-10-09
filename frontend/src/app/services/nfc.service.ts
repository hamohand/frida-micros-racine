import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, Subscriber } from 'rxjs';

export interface NfcData {
  nom?: string;
  prenom?: string;
  latines?: string;
  prenomLatines?: string;
  nomArabe?: string;
  prenomArabe?: string;
  dateNaissance?: string;
  nin?: string;
  documentNumber?: string;
  sexe?: string;
  photoBase64?: string;
  rawJson?: any;
}

@Injectable({
  providedIn: 'root'
})
export class NfcService {
  // L'adresse de l'agent local dz-eid (lecteur USB). On utilise l'hôte actuel pour permettre l'accès depuis le mobile (réseau local).
  private readonly USB_AGENT_URL = `http://${window.location.hostname}:8989`;

  constructor(private http: HttpClient) {}

  /**
   * Vérifie si le lecteur NFC USB (Agent Local dz-eid) est présent et actif sur ce PC.
   */
  public checkUsbAgentAvailable(): Observable<boolean> {
    return this.http.get<any>(`${this.USB_AGENT_URL}/v1/status`).pipe(
      map(res => {
        console.log("Agent dz-eid Status:", res);
        return res && (res.product === 'dz-eid-agent' || res.readers !== undefined);
      }),
      catchError(() => of(false)) // Si l'agent n'est pas lancé, on passe silencieusement à false
    );
  }

  /**
   * Demande à l'Agent Local dz-eid de lire la puce NFC via le lecteur USB.
   * Retourne l'identité complète (arabe, latin, NIN, photo, dates) au contrat IdentityRecord 1.1.
   */
  public readViaUsb(documentNumber: string, dateOfBirth: string, dateOfExpiry: string): Observable<NfcData> {
    const payload = { documentNumber, dateOfBirth, dateOfExpiry };
    return this.http.post<any>(`${this.USB_AGENT_URL}/v1/read`, payload).pipe(
      map(record => {
        const holder = record.holder || {};
        const doc = record.document || {};
        const photo = record.photo || {};

        return {
          nom: holder.lastNameLatin || holder.lastNameArabic || '',
          prenom: holder.firstNameLatin || holder.firstNameArabic || '',
          latines: holder.lastNameLatin || '',
          prenomLatines: holder.firstNameLatin || '',
          nomArabe: holder.lastNameArabic || '',
          prenomArabe: holder.firstNameArabic || '',
          dateNaissance: holder.dateOfBirth || '',
          nin: holder.nin || '',
          documentNumber: doc.number || documentNumber,
          sexe: (holder.sex || '').toUpperCase().startsWith('M') ? 'M' : 'F',
          photoBase64: photo.base64 || '',
          rawJson: record
        } as NfcData;
      })
    );
  }

  /**
   * Écoute le flux SSE (Server-Sent Events) pour attendre les données envoyées par l'application Mobile.
   * Retourne un Observable qui se complète automatiquement dès réception.
   * 
   * @param apiUrl L'URL de l'API backend (ex: http://192.168.1.47/api)
   * @param sessionId L'UUID unique de la session (généré pour le QR Code)
   */
  public listenToMobileNfc(apiUrl: string, sessionId: string): Observable<any> {
    return new Observable<any>((subscriber: Subscriber<any>) => {
      const streamUrl = `${apiUrl}/nfc-session/${sessionId}/stream`;
      const eventSource = new EventSource(streamUrl);

      eventSource.addEventListener('INIT', (event) => {
        console.log('SSE Connecté (Attente du Mobile NFC):', event);
      });

      eventSource.addEventListener('MRZ_DATA', (event: MessageEvent) => {
        try {
          const mrzData = JSON.parse(event.data);
          subscriber.next({ type: 'MRZ_DATA', payload: mrzData });
        } catch (e) {
          console.error(e);
        }
      });

      eventSource.addEventListener('NFC_DATA', (event: MessageEvent) => {
        try {
          const rawData = JSON.parse(event.data);
          const data: NfcData = {
            ...rawData,
            nom: rawData.nom || rawData.primaryIdentifier,
            prenom: rawData.prenom || rawData.secondaryIdentifier,
            nomArabe: rawData.nomArabe || rawData.nom_arabe,
            prenomArabe: rawData.prenomArabe || rawData.prenom_arabe
          };
          // Dès qu'on reçoit la donnée, on l'émet et on coupe la connexion
          subscriber.next({ type: 'NFC_DATA', payload: data });
          subscriber.complete();
          eventSource.close();
        } catch (e) {
          subscriber.error('Erreur lors du parsing JSON du flux NFC Mobile');
          eventSource.close();
        }
      });

      eventSource.onerror = (error) => {
        console.error('Erreur EventSource Mobile NFC', error);
        subscriber.error(error);
        eventSource.close();
      };

      // Si le composant Angular est détruit, on ferme proprement le SSE
      return () => {
        eventSource.close();
      };
    });
  }
}
