import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, Subscriber } from 'rxjs';

export interface NfcData {
  nom?: string;
  prenom?: string;
  dateNaissance?: string;
  nin?: string;
  photoBase64?: string;
  rawJson?: any;
}

@Injectable({
  providedIn: 'root'
})
export class NfcService {
  // L'adresse de l'agent local (lecteur USB)
  private readonly USB_AGENT_URL = 'http://localhost:8088';

  constructor(private http: HttpClient) {}

  /**
   * Vérifie si le lecteur NFC USB (Agent Local) est présent et actif sur ce PC.
   * Très rapide, permet de décider s'il faut afficher le QR Code ou non.
   */
  public checkUsbAgentAvailable(): Observable<boolean> {
    return this.http.get(`${this.USB_AGENT_URL}/status`, { responseType: 'text' }).pipe(
      map(() => true),
      catchError(() => of(false)) // Si l'agent n'est pas lancé, on passe silencieusement à false
    );
  }

  /**
   * Demande à l'Agent Local de lire la puce NFC via le lecteur USB.
   * Les clés BAC sont indispensables pour déverrouiller la puce.
   */
  public readViaUsb(documentNumber: string, dateOfBirth: string, dateOfExpiry: string): Observable<NfcData> {
    const payload = { documentNumber, dateOfBirth, dateOfExpiry };
    return this.http.post<NfcData>(`${this.USB_AGENT_URL}/api/read-nfc`, payload);
  }

  /**
   * Écoute le flux SSE (Server-Sent Events) pour attendre les données envoyées par l'application Mobile.
   * Retourne un Observable qui se complète automatiquement dès réception.
   * 
   * @param apiUrl L'URL de l'API backend (ex: http://192.168.1.47/api)
   * @param sessionId L'UUID unique de la session (généré pour le QR Code)
   */
  public listenToMobileNfc(apiUrl: string, sessionId: string): Observable<NfcData> {
    return new Observable<NfcData>((subscriber: Subscriber<NfcData>) => {
      const streamUrl = `${apiUrl}/nfc-session/${sessionId}/stream`;
      const eventSource = new EventSource(streamUrl);

      eventSource.addEventListener('INIT', (event) => {
        console.log('SSE Connecté (Attente du Mobile NFC):', event);
      });

      eventSource.addEventListener('NFC_DATA', (event: MessageEvent) => {
        try {
          const data = JSON.parse(event.data) as NfcData;
          // Dès qu'on reçoit la donnée, on l'émet et on coupe la connexion
          subscriber.next(data);
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
