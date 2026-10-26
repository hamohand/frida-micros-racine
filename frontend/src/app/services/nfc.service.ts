import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, catchError, map, of, Subscriber, switchMap } from 'rxjs';
import { io, Socket } from 'socket.io-client';

export interface NfcVerificationInfo {
  accessMethod?: string;
  passiveAuthentication?: {
    dataIntegrity?: string;
    signature?: string;
    certificateChain?: string;
    summary?: string;
    details?: string[];
  };
  activeAuthentication?: {
    result?: string;
    algorithm?: string;
    summary?: string;
  };
}

export interface NfcData {
  nom?: string;
  prenom?: string;
  latines?: string;
  prenomLatines?: string;
  nomArabe?: string;
  prenomArabe?: string;
  dateNaissance?: string;
  lieuNaissance?: string;
  lieuNaissanceLatin?: string;
  lieuNaissanceArabe?: string;
  nin?: string;
  documentNumber?: string;
  documentType?: string;
  documentNameLatin?: string;
  documentNameArabic?: string;
  delivreLe?: string;
  expireLe?: string;
  delivrePar?: string;
  delivreParLatin?: string;
  delivreParArabe?: string;
  sexe?: string;
  sexeArabe?: string;
  groupeSanguin?: string;
  nationalite?: string;
  adresse?: string;
  photoBase64?: string;
  photoMimeType?: string;
  signatureBase64?: string;
  signatureMimeType?: string;
  verification?: NfcVerificationInfo;
  warnings?: string[];
  rawJson?: any;
}

export interface UsbReaderInfo {
  name: string;
  cardPresent: boolean;
}

export interface UsbAgentStatus {
  product: string;
  version: string;
  busy?: boolean;
  readers: UsbReaderInfo[];
  cscaCertificates?: number;
  mobileRelay?: boolean;
  lanAddresses?: string[];
}

export interface UsbMrzRelaySession {
  sessionId: string;
  host: string;
  addresses: string[];
  expiresInSeconds: number;
  qrSvg: string;
}

export interface MobileE2eeSessionInfo {
  sessionId: string;
  qrPayload: string;
  mobileRelayUrl: string;
  host: string;
  lanPort: number;
  addresses: string[];
  mode: 'dzeid-relay' | 'frida-backend';
  updateHost: (newHost: string) => string;
  disconnect: () => void;
}

@Injectable({
  providedIn: 'root'
})
export class NfcService {
  // Agent local dz-eid (lecteur USB + passerelle LAN port 8989)
  private readonly USB_AGENT_URL = 'http://127.0.0.1:8989';
  // Serveur relais E2EE dz-eid-relay (port 3000)
  private readonly E2EE_RELAY_URL = 'http://localhost:3000';

  constructor(private http: HttpClient) {}

  /**
   * Récupère l'état complet de l'Agent Local dz-eid (version, lecteurs, présence carte, adresses LAN).
   */
  public getUsbAgentStatus(): Observable<UsbAgentStatus | null> {
    return this.http.get<UsbAgentStatus>(`${this.USB_AGENT_URL}/v1/status`).pipe(
      map(res => (res && (res.product === 'dz-eid-agent' || res.readers !== undefined)) ? res : null),
      catchError(() => of(null))
    );
  }

  /**
   * Vérifie si le lecteur NFC USB (Agent Local dz-eid) est présent et actif sur ce PC.
   */
  public checkUsbAgentAvailable(): Observable<boolean> {
    return this.getUsbAgentStatus().pipe(map(status => status !== null));
  }

  /**
   * IP actuelles du poste sur le réseau local, détectées par dz-eid-agent (port 8989)
   * ou par dz-eid-relay (port 3000), cartes physiques (Wi-Fi / Ethernet) en premier.
   */
  public getLanAddresses(): Observable<string[]> {
    return this.getUsbAgentStatus().pipe(
      switchMap(status => {
        const agentIps = Array.isArray(status?.lanAddresses)
          ? status!.lanAddresses!.filter((a: any) => typeof a === 'string' && a)
          : [];
        if (agentIps.length > 0) {
          return of(agentIps);
        }
        return this.http.get<any>(`${this.E2EE_RELAY_URL}/health`).pipe(
          map(health => {
            if (Array.isArray(health?.lanAddresses) && health.lanAddresses.length > 0) {
              return health.lanAddresses.filter((a: any) => typeof a === 'string' && a);
            }
            return health?.lanIp ? [String(health.lanIp)] : [];
          }),
          catchError(() => of([] as string[]))
        );
      })
    );
  }

  /**
   * Connexion WebSocket temps réel aux événements de dz-eid-agent (ws://127.0.0.1:8989/v1/events) :
   * HELLO, READERS_CHANGED, CARD_INSERTED, CARD_REMOVED, WAITING_FOR_CARD, PROGRESS, READ_COMPLETED, READ_FAILED.
   */
  public connectUsbEvents(): Observable<any> {
    return new Observable<any>((subscriber: Subscriber<any>) => {
      const wsUrl = this.USB_AGENT_URL.replace(/^http/, 'ws') + '/v1/events';
      let ws: WebSocket | null = null;
      try {
        ws = new WebSocket(wsUrl);
      } catch (err) {
        subscriber.error(err);
        return;
      }

      ws.onopen = () => {
        subscriber.next({ type: 'WS_OPEN' });
      };

      ws.onmessage = (m: MessageEvent) => {
        try {
          const ev = JSON.parse(m.data);
          subscriber.next(ev);
        } catch (e) {
          console.error('Erreur parsing WS dz-eid-agent:', e);
        }
      };

      ws.onerror = (err) => {
        subscriber.next({ type: 'WS_ERROR', error: err });
      };

      ws.onclose = () => {
        subscriber.next({ type: 'WS_CLOSE' });
      };

      return () => {
        if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
          ws.close();
        }
      };
    });
  }

  /**
   * Ouvre une session de relais MRZ (« Scanner la MRZ avec la douchette ») directement sur dz-eid-agent (port 8989).
   */
  public openUsbMrzRelay(host?: string | null): Observable<UsbMrzRelaySession> {
    const body = host ? { host } : {};
    return this.http.post<UsbMrzRelaySession>(`${this.USB_AGENT_URL}/v1/relay`, body);
  }

  /**
   * Écoute le flux SSE du relais MRZ de dz-eid-agent (/v1/relay/{sessionId}/stream).
   */
  public listenUsbMrzRelay(sessionId: string): Observable<any> {
    return new Observable<any>((subscriber: Subscriber<any>) => {
      const streamUrl = `${this.USB_AGENT_URL}/v1/relay/${encodeURIComponent(sessionId)}/stream`;
      const eventSource = new EventSource(streamUrl);

      eventSource.addEventListener('MRZ_DATA', (event: MessageEvent) => {
        try {
          const mrzData = JSON.parse(event.data);
          subscriber.next({ type: 'MRZ_DATA', payload: mrzData });
          subscriber.complete();
          eventSource.close();
        } catch (e) {
          subscriber.error(e);
          eventSource.close();
        }
      });

      eventSource.addEventListener('RELAY_ERROR', (event: MessageEvent) => {
        subscriber.error(new Error(event.data || 'Erreur du relais MRZ'));
        eventSource.close();
      });

      eventSource.onerror = (err) => {
        subscriber.error(err);
        eventSource.close();
      };

      return () => {
        eventSource.close();
      };
    });
  }

  /**
   * Demande à l'Agent Local dz-eid de lire la puce NFC via le lecteur USB (POST /v1/read).
   * Retourne l'identité complète (arabe, latin, NIN, photo, signature, vérifications CSCA/AA)
   * au contrat IdentityRecord 1.1 (sans preuve de vie).
   */
  public readViaUsb(
    documentNumber: string,
    dateOfBirth: string,
    dateOfExpiry: string,
    mrz?: string,
    readPhoto = false,
    readSignature = false,
    includeRaw = false
  ): Observable<NfcData> {
    const payload: any = {
      documentNumber: documentNumber.trim(),
      dateOfBirth: dateOfBirth.trim(),
      dateOfExpiry: dateOfExpiry.trim(),
      readPhoto,
      readSignature,
      includeRaw
    };
    if (mrz && mrz.replace(/\s/g, '').length >= 72) {
      payload.mrz = mrz.trim();
    }
    return this.http.post<any>(`${this.USB_AGENT_URL}/v1/read`, payload).pipe(
      map(record => this.mapIdentityRecordToNfcData(record, documentNumber))
    );
  }

  /**
   * Convertit un contrat IdentityRecord v1.1 (ou format hérité) en NfcData filtré pour Frida :
   * supprime systématiquement la photo (DG2), la signature (DG7), et toute donnée de preuve de vie (liveness).
   */
  public mapIdentityRecordToNfcData(record: any, fallbackDocNumber = ''): NfcData {
    if (!record) return {};
    const cleanRecord = { ...record };
    // Filtrage strict Frida : pas de photo, pas de signature, pas de preuve de vie
    delete cleanRecord.photo;
    delete cleanRecord.signatureImage;
    delete cleanRecord.liveness;
    delete cleanRecord.proof;
    delete cleanRecord.raw;

    if (cleanRecord.holder || cleanRecord.document) {
      const holder = cleanRecord.holder || {};
      const doc = cleanRecord.document || {};
      const verif = cleanRecord.verification || {};

      return {
        nom: holder.lastNameArabic || holder.lastNameLatin || '',
        prenom: holder.firstNameArabic || holder.firstNameLatin || '',
        latines: holder.lastNameLatin || '',
        prenomLatines: holder.firstNameLatin || '',
        nomArabe: holder.lastNameArabic || '',
        prenomArabe: holder.firstNameArabic || '',
        dateNaissance: holder.dateOfBirth || '',
        lieuNaissance: holder.placeOfBirthArabic || holder.placeOfBirthLatin || '',
        lieuNaissanceLatin: holder.placeOfBirthLatin || '',
        lieuNaissanceArabe: holder.placeOfBirthArabic || '',
        nin: holder.nin || '',
        documentNumber: doc.number || doc.code || fallbackDocNumber || '',
        documentType: doc.type || 'ID',
        documentNameLatin: doc.nameLatin || doc.type || '',
        documentNameArabic: doc.nameArabic || '',
        delivreLe: doc.dateOfIssue || '',
        expireLe: doc.dateOfExpiry || '',
        delivrePar: doc.issuingAuthorityArabic || doc.issuingAuthorityLatin || '',
        delivreParLatin: doc.issuingAuthorityLatin || '',
        delivreParArabe: doc.issuingAuthorityArabic || '',
        sexe: (holder.sex || '').toUpperCase().startsWith('M') ? 'M' : (holder.sex ? 'F' : ''),
        sexeArabe: holder.sexArabic || '',
        nationalite: holder.nationality || '',
        verification: {
          accessMethod: verif.accessMethod,
          passiveAuthentication: verif.passiveAuthentication ? {
            dataIntegrity: verif.passiveAuthentication.dataIntegrity,
            signature: verif.passiveAuthentication.signature,
            certificateChain: verif.passiveAuthentication.certificateChain,
            summary: verif.passiveAuthentication.summary,
            details: verif.passiveAuthentication.details
          } : undefined,
          activeAuthentication: verif.activeAuthentication ? {
            result: verif.activeAuthentication.result,
            algorithm: verif.activeAuthentication.algorithm,
            summary: verif.activeAuthentication.summary
          } : undefined
        },
        warnings: Array.isArray(cleanRecord.warnings) ? cleanRecord.warnings : [],
        rawJson: cleanRecord
      };
    }

    // Format hérité
    delete cleanRecord.photoBase64;
    delete cleanRecord.signatureBase64;
    return {
      ...cleanRecord,
      nom: cleanRecord.nomArabe || cleanRecord.nom_arabe || cleanRecord.nom || cleanRecord.primaryIdentifier || '',
      prenom: cleanRecord.prenomArabe || cleanRecord.prenom_arabe || cleanRecord.prenom || cleanRecord.secondaryIdentifier || '',
      latines: cleanRecord.latines || cleanRecord.primaryIdentifier || cleanRecord.nom || '',
      prenomLatines: cleanRecord.prenomLatines || cleanRecord.secondaryIdentifier || cleanRecord.prenom || '',
      nomArabe: cleanRecord.nomArabe || cleanRecord.nom_arabe || '',
      prenomArabe: cleanRecord.prenomArabe || cleanRecord.prenom_arabe || '',
      dateNaissance: cleanRecord.dateNaissance || cleanRecord.dateOfBirth || '',
      nin: cleanRecord.nin || cleanRecord.nin_dg11 || cleanRecord.personalNumber || '',
      documentNumber: cleanRecord.documentNumber || fallbackDocNumber || '',
      sexe: (cleanRecord.sexe || cleanRecord.gender || '').toUpperCase().startsWith('M') ? 'M' : 'F',
      rawJson: cleanRecord
    };
  }

  /**
   * Initialise une session E2EE (ECDH P-256 + AES-256-GCM) pour la lecture NFC Mobile
   * via dz-eid-relay (port 3000 / passerelle 8989), avec repli automatique sur le backend Frida
   * si dz-eid-relay n'est pas actif.
   */
  public async initMobileE2eeSession(
    fallbackAddresses: string[] = [],
    onCardDataReceived: (data: NfcData, sessionId: string) => void,
    onError?: (err: any) => void
  ): Promise<MobileE2eeSessionInfo> {
    // 1. Générer la paire de clés éphémère ECDH P-256 dans le navigateur
    const keyPair = await crypto.subtle.generateKey(
      { name: 'ECDH', namedCurve: 'P-256' },
      true,
      ['deriveBits']
    );
    const pubKeyRaw = await crypto.subtle.exportKey('raw', keyPair.publicKey);
    const pubKeyB64 = this.arrayBufferToBase64(pubKeyRaw);

    // 2. Tenter la connexion à dz-eid-relay (http://localhost:3000) comme dans dz-eid Standard
    try {
      const relaySession = await this.connectToDzEidRelay(keyPair, pubKeyB64, fallbackAddresses, onCardDataReceived);
      return relaySession;
    } catch (relayErr) {
      console.warn('dz-eid-relay (port 3000) non disponible, basculement sur le relais E2EE Frida :', relayErr);
      return this.connectToFridaE2eeBackend(keyPair, pubKeyB64, fallbackAddresses, onCardDataReceived, onError);
    }
  }

  private connectToDzEidRelay(
    keyPair: CryptoKeyPair,
    pubKeyB64: string,
    fallbackAddresses: string[],
    onCardDataReceived: (data: NfcData, sessionId: string) => void
  ): Promise<MobileE2eeSessionInfo> {
    return new Promise((resolve, reject) => {
      const socket: Socket = io(this.E2EE_RELAY_URL, {
        autoConnect: false,
        timeout: 2500,
        reconnection: false
      });

      const timer = setTimeout(() => {
        socket.disconnect();
        reject(new Error('Timeout connexion dz-eid-relay'));
      }, 2800);

      socket.on('connect', () => {
        socket.emit('request_session');
      });

      socket.once('session_created', (data: {
        sessionId: string;
        lanIp?: string | null;
        lanPort?: number;
        lanUrl?: string | null;
        lanAddresses?: string[];
      }) => {
        clearTimeout(timer);
        const sessionId = data.sessionId;
        const addresses = Array.from(new Set([
          ...(Array.isArray(data.lanAddresses) ? data.lanAddresses : []),
          ...(data.lanIp ? [data.lanIp] : []),
          ...fallbackAddresses
        ])).filter(Boolean);

        const lanPort = data.lanPort || 3000;
        const portSuffix = window.location.port ? `:${window.location.port}` : '';
        let currentHost = addresses[0] || data.lanIp || window.location.hostname;
        let mobileRelayUrl = currentHost
          ? `${window.location.protocol}//${currentHost}${portSuffix}`
          : (data.lanUrl || this.E2EE_RELAY_URL);

        const buildPayload = (hostIp: string) => {
          const dzeidUrl = `http://${hostIp}:${lanPort}`;
          return JSON.stringify({
            u: dzeidUrl,
            s: sessionId,
            k: pubKeyB64
          });
        };

        // Écoute simultanée sur le backend Frida (pour frida-mobile-nfc) et sur dz-eid-relay (pour dz-eid Mobile)
        const fridaSub = this.listenToMobileNfc('/api', sessionId).subscribe({
          next: async (event: any) => {
            if (event.type === 'ENCRYPTED_NFC_DATA') {
              try {
                const decryptedRecord = await this.decryptE2eePayload(keyPair.privateKey, event.payload);
                socket.disconnect();
                onCardDataReceived(this.mapIdentityRecordToNfcData(decryptedRecord), sessionId);
              } catch (e) {
                console.error('Erreur déchiffrement E2EE via backend Frida :', e);
              }
            } else if (event.type === 'NFC_DATA') {
              socket.disconnect();
              onCardDataReceived(this.mapIdentityRecordToNfcData(event.payload), sessionId);
            }
          }
        });

        socket.once('card_data_received', async (encryptedPayload: { mobPubKeyB64: string; ivB64: string; ciphertextB64: string }) => {
          try {
            const decryptedRecord = await this.decryptE2eePayload(keyPair.privateKey, encryptedPayload);
            fridaSub.unsubscribe();
            socket.disconnect();
            const nfcData = this.mapIdentityRecordToNfcData(decryptedRecord);
            onCardDataReceived(nfcData, sessionId);
          } catch (err) {
            console.error('Erreur déchiffrement E2EE Mobile NFC :', err);
          }
        });

        const sessionInfo: MobileE2eeSessionInfo = {
          sessionId,
          qrPayload: buildPayload(currentHost),
          mobileRelayUrl,
          host: currentHost,
          lanPort,
          addresses,
          mode: 'dzeid-relay',
          updateHost: (newHost: string) => {
            currentHost = newHost.trim();
            mobileRelayUrl = `${window.location.protocol}//${currentHost}${portSuffix}`;
            sessionInfo.host = currentHost;
            sessionInfo.mobileRelayUrl = mobileRelayUrl;
            sessionInfo.qrPayload = buildPayload(currentHost);
            return sessionInfo.qrPayload;
          },
          disconnect: () => {
            fridaSub.unsubscribe();
            socket.disconnect();
          }
        };

        resolve(sessionInfo);
      });

      socket.on('connect_error', (err) => {
        clearTimeout(timer);
        socket.disconnect();
        reject(err);
      });

      socket.connect();
    });
  }

  private connectToFridaE2eeBackend(
    keyPair: CryptoKeyPair,
    pubKeyB64: string,
    fallbackAddresses: string[],
    onCardDataReceived: (data: NfcData, sessionId: string) => void,
    onError?: (err: any) => void
  ): MobileE2eeSessionInfo {
    const sessionId = crypto.randomUUID ? crypto.randomUUID() : Math.random().toString(36).substring(2) + Date.now().toString(36);
    const hote = window.location.hostname;
    const estLocal = hote === 'localhost' || hote === '127.0.0.1';
    const addresses = Array.from(new Set([
      ...(!estLocal ? [hote] : []),
      ...fallbackAddresses
    ])).filter(Boolean);

    const portNum = window.location.port ? Number(window.location.port) : (window.location.protocol === 'https:' ? 443 : 80);
    let currentHost = addresses[0] || hote;
    const portSuffix = window.location.port ? `:${window.location.port}` : '';
    let mobileRelayUrl = `${window.location.protocol}//${currentHost}${portSuffix}`;

    const buildPayload = (hostIp: string) => {
      const fridaBaseUrl = `${window.location.protocol}//${hostIp}${portSuffix}`;
      return JSON.stringify({
        u: fridaBaseUrl,
        s: sessionId,
        k: pubKeyB64
      });
    };

    const sub = this.listenToMobileNfc('/api', sessionId).subscribe({
      next: async (event: any) => {
        if (event.type === 'ENCRYPTED_NFC_DATA') {
          try {
            const decryptedRecord = await this.decryptE2eePayload(keyPair.privateKey, event.payload);
            const nfcData = this.mapIdentityRecordToNfcData(decryptedRecord);
            onCardDataReceived(nfcData, sessionId);
          } catch (e) {
            console.error('Erreur déchiffrement E2EE via backend Frida :', e);
            if (onError) onError(e);
          }
        } else if (event.type === 'NFC_DATA') {
          onCardDataReceived(this.mapIdentityRecordToNfcData(event.payload), sessionId);
        }
      },
      error: (err) => {
        if (onError) onError(err);
      }
    });

    const sessionInfo: MobileE2eeSessionInfo = {
      sessionId,
      qrPayload: buildPayload(currentHost),
      mobileRelayUrl,
      host: currentHost,
      lanPort: portNum,
      addresses,
      mode: 'frida-backend',
      updateHost: (newHost: string) => {
        currentHost = newHost.trim();
        mobileRelayUrl = `${window.location.protocol}//${currentHost}${portSuffix}`;
        sessionInfo.host = currentHost;
        sessionInfo.mobileRelayUrl = mobileRelayUrl;
        sessionInfo.qrPayload = buildPayload(currentHost);
        return sessionInfo.qrPayload;
      },
      disconnect: () => {
        sub.unsubscribe();
      }
    };

    return sessionInfo;
  }

  /**
   * Déchiffre la charge utile E2EE (ECDH P-256 + SHA-256 + AES-256-GCM) reçue de dz-eid Mobile.
   */
  public async decryptE2eePayload(
    privateKey: CryptoKey,
    payload: { mobPubKeyB64: string; ivB64: string; ciphertextB64: string }
  ): Promise<any> {
    const mobPubKeyRaw = this.base64ToArrayBuffer(payload.mobPubKeyB64);
    const mobPubKey = await crypto.subtle.importKey(
      'raw',
      mobPubKeyRaw,
      { name: 'ECDH', namedCurve: 'P-256' },
      true,
      []
    );

    const sharedSecret = await crypto.subtle.deriveBits(
      { name: 'ECDH', public: mobPubKey },
      privateKey,
      256
    );

    const aesKeyMaterial = await crypto.subtle.digest('SHA-256', sharedSecret);
    const aesKey = await crypto.subtle.importKey(
      'raw',
      aesKeyMaterial,
      { name: 'AES-GCM' },
      false,
      ['decrypt']
    );

    const iv = this.base64ToArrayBuffer(payload.ivB64);
    const ciphertext = this.base64ToArrayBuffer(payload.ciphertextB64);

    const decryptedBuffer = await crypto.subtle.decrypt(
      { name: 'AES-GCM', iv: new Uint8Array(iv) },
      aesKey,
      ciphertext
    );

    const jsonStr = new TextDecoder().decode(decryptedBuffer);
    return JSON.parse(jsonStr);
  }

  /**
   * Écoute le flux SSE (Server-Sent Events) du backend Frida (/api/nfc-session/{sessionId}/stream).
   */
  public listenToMobileNfc(apiUrl: string, sessionId: string): Observable<any> {
    return new Observable<any>((subscriber: Subscriber<any>) => {
      const streamUrl = `${apiUrl}/nfc-session/${sessionId}/stream`;
      const eventSource = new EventSource(streamUrl);

      eventSource.addEventListener('INIT', () => {});

      eventSource.addEventListener('MRZ_DATA', (event: MessageEvent) => {
        try {
          const mrzData = JSON.parse(event.data);
          subscriber.next({ type: 'MRZ_DATA', payload: mrzData });
        } catch (e) {
          console.error(e);
        }
      });

      eventSource.addEventListener('ENCRYPTED_NFC_DATA', (event: MessageEvent) => {
        try {
          const encryptedData = JSON.parse(event.data);
          subscriber.next({ type: 'ENCRYPTED_NFC_DATA', payload: encryptedData });
          subscriber.complete();
          eventSource.close();
        } catch (e) {
          subscriber.error('Erreur lors du parsing JSON du flux E2EE Mobile');
          eventSource.close();
        }
      });

      eventSource.addEventListener('NFC_DATA', (event: MessageEvent) => {
        try {
          const rawData = JSON.parse(event.data);
          const data = this.mapIdentityRecordToNfcData(rawData);
          subscriber.next({ type: 'NFC_DATA', payload: data });
          subscriber.complete();
          eventSource.close();
        } catch (e) {
          subscriber.error('Erreur lors du parsing JSON du flux NFC Mobile');
          eventSource.close();
        }
      });

      eventSource.onerror = (error) => {
        subscriber.error(error);
        eventSource.close();
      };

      return () => {
        eventSource.close();
      };
    });
  }

  /**
   * Normalise les confusions OCR classiques (O->0, I->1, Z->2, S->5, G->6, T->7, B->8) sur les chiffres MRZ.
   */
  public normalizeMrzDigits(s: string): string {
    if (!s) return '';
    const mapChar: Record<string, string> = {
      O: '0', Q: '0', D: '0', U: '0',
      I: '1', L: '1', J: '1',
      Z: '2',
      S: '5',
      G: '6',
      T: '7',
      B: '8'
    };
    return s
      .toUpperCase()
      .trim()
      .split('')
      .map(c => mapChar[c] || c)
      .join('');
  }

  /**
   * Extrait { doc, dob, doe } depuis une chaîne MRZ complète (TD1 3 lignes / 90 car. ou TD3 2 lignes / 88 car.).
   */
  public parseMrzString(rawVal: string): { doc: string; dob: string; doe: string } | null {
    if (!rawVal) return null;
    const upper = rawVal.toUpperCase();
    const compact = upper.replace(/\s/g, '');
    let doc = '';
    let dob = '';
    let doe = '';

    if (compact.length === 90) {
      doc = compact.substring(5, 14);
      dob = compact.substring(30, 36);
      doe = compact.substring(38, 44);
    } else if (compact.length === 88) {
      doc = compact.substring(44, 53);
      dob = compact.substring(57, 63);
      doe = compact.substring(65, 71);
    } else {
      const lines = upper
        .trim()
        .split(/[\r\n]+/)
        .map(l => l.replace(/\s/g, ''))
        .filter(Boolean);
      if (lines.length >= 2 && lines[0].startsWith('I') && lines[0].length >= 14 && lines[1].length >= 14) {
        doc = lines[0].substring(5, 14);
        dob = lines[1].substring(0, 6);
        doe = lines[1].substring(8, 14);
      } else if (lines.length >= 2 && lines[0].startsWith('P') && lines[1].length >= 27) {
        doc = lines[1].substring(0, 9);
        dob = lines[1].substring(13, 19);
        doe = lines[1].substring(21, 27);
      }
    }

    const cleanDoc = doc.replace(/</g, '');
    if (!cleanDoc) return null;
    const isDzaId = compact.startsWith('IDDZA') || upper.trim().startsWith('IDDZA');
    return {
      doc: isDzaId ? this.normalizeMrzDigits(cleanDoc) : cleanDoc,
      dob: this.normalizeMrzDigits(dob.replace(/</g, '')),
      doe: this.normalizeMrzDigits(doe.replace(/</g, ''))
    };
  }

  private arrayBufferToBase64(buffer: ArrayBuffer): string {
    let binary = '';
    const bytes = new Uint8Array(buffer);
    for (let i = 0; i < bytes.byteLength; i++) {
      binary += String.fromCharCode(bytes[i]);
    }
    return btoa(binary);
  }

  private base64ToArrayBuffer(base64: string): ArrayBuffer {
    const binary = atob(base64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i);
    }
    return bytes.buffer;
  }
}
