import { Component, EventEmitter, Output, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { QRCodeModule } from 'angularx-qrcode';
import { v4 as uuidv4 } from 'uuid';
import { ParametresService } from '../../../services/parametres.service';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-nfc-scanner-modal',
  standalone: true,
  imports: [CommonModule, QRCodeModule],
  template: `
    <div class="modal-overlay" (click)="close()">
      <div class="modal-content" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h2>📲 Scanner NFC depuis le Mobile</h2>
          <button class="close-btn" (click)="close()">✕</button>
        </div>

        <!-- Adresse réseau locale non configurée : pas de QR code à générer -->
        <div class="modal-body config-manquante" *ngIf="adresseNonConfiguree">
          <span class="material-icons warning-icon">wifi_off</span>
          <h3>Adresse réseau non configurée</h3>
          <p>
            Le téléphone ne peut pas joindre ce poste via « localhost », qui ne désigne que lui-même
            une fois sur le réseau Wi-Fi. Il faut d'abord donner l'adresse de ce poste sur le réseau local.
          </p>
          <p *ngIf="estMaitre">
            Page <b>Paramètres</b> → « Adresse réseau locale du poste » (trouvée avec <code>ipconfig</code>).
          </p>
          <p *ngIf="!estMaitre">
            Demandez à un compte Maître de la renseigner, page <b>Paramètres</b>.
          </p>
        </div>

        <div class="modal-body" *ngIf="!adresseNonConfiguree && !successData">
          <p class="instructions">
            <strong>1.</strong> Connectez votre mobile au même réseau Wi-Fi.<br>
            <strong>2.</strong> Ouvrez l'application <b>Frida Mobile</b>.<br>
            <strong>3.</strong> Scannez ce QR Code.
          </p>

          <div class="qr-container" *ngIf="qrData">
            <qrcode [qrdata]="qrData" [width]="256" [errorCorrectionLevel]="'M'"></qrcode>
          </div>

          <div class="loading-state" *ngIf="!qrData">
            <span class="spinner"></span> Génération de la session...
          </div>

          <div class="listening-state" *ngIf="qrData">
            <span class="spinner"></span> <i>En attente des données du mobile...</i>
          </div>
        </div>

        <div class="modal-body success" *ngIf="successData">
          <span class="material-icons success-icon">check_circle</span>
          <h3>Lecture Réussie !</h3>
          <p>{{ successData.nom }} {{ successData.prenom }}</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .modal-overlay {
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0, 0, 0, 0.7);
      backdrop-filter: blur(5px);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }
    .modal-content {
      background: #1e293b;
      border: 1px solid rgba(78, 204, 163, 0.3);
      border-radius: 12px;
      width: 400px;
      max-width: 90vw;
      box-shadow: 0 10px 25px rgba(0,0,0,0.5);
      color: white;
    }
    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1rem 1.5rem;
      border-bottom: 1px solid rgba(255,255,255,0.1);
    }
    .modal-header h2 { margin: 0; font-size: 1.25rem; color: #4ecca3; }
    .close-btn {
      background: none; border: none; color: white;
      font-size: 1.5rem; cursor: pointer; opacity: 0.7;
    }
    .close-btn:hover { opacity: 1; }
    .modal-body { padding: 1.5rem; text-align: center; }
    .instructions {
      text-align: left;
      background: rgba(255,255,255,0.05);
      padding: 1rem;
      border-radius: 8px;
      margin-bottom: 1.5rem;
      line-height: 1.6;
      font-size: 0.95rem;
    }
    .qr-container {
      background: white;
      padding: 1rem;
      border-radius: 8px;
      display: inline-block;
      margin-bottom: 1rem;
    }
    .listening-state { color: #ffb84d; margin-top: 1rem; font-size: 0.9rem; }
    .success { text-align: center; padding: 2rem; }
    .success-icon { font-size: 4rem; color: #4ecca3; margin-bottom: 1rem; }
    .config-manquante { text-align: left; }
    .warning-icon { font-size: 3rem; color: #ffb84d; display: block; text-align: center; margin-bottom: 0.5rem; }
    .config-manquante h3 { text-align: center; margin: 0 0 1rem; }
    .config-manquante p { line-height: 1.5; font-size: 0.95rem; }
    .config-manquante code { background: rgba(255, 255, 255, 0.1); padding: 0.1rem 0.4rem; border-radius: 4px; }

    .spinner {
      display: inline-block;
      width: 16px; height: 16px;
      border: 2px solid rgba(255,255,255,0.3);
      border-top-color: white;
      border-radius: 50%;
      animation: spin 1s linear infinite;
      vertical-align: middle;
      margin-right: 8px;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class NfcScannerModalComponent implements OnInit, OnDestroy {
  @Output() closeModal = new EventEmitter<void>();
  @Output() nfcDataReceived = new EventEmitter<any>();

  sessionId: string = '';
  qrData: string = '';
  successData: any = null;
  adresseNonConfiguree = false;
  estMaitre = false;
  private eventSource: EventSource | null = null;

  constructor(private parametresService: ParametresService, private authService: AuthService) {}

  ngOnInit() {
    this.sessionId = uuidv4();
    this.estMaitre = this.authService.isMaitre();

    this.parametresService.lire().subscribe({
      next: (p) => {
        const adresse = (p.adresseReseauLocale || '').trim();
        if (!adresse) {
          this.adresseNonConfiguree = true;
          return;
        }
        // Même schéma et port que ceux utilisés pour accéder à FRIDA depuis ce navigateur
        // (nginx proxifie déjà /api/ vers le backend) : seul « localhost » doit être remplacé,
        // car le téléphone ne le résoudrait qu'à lui-même sur le réseau Wi-Fi.
        const port = window.location.port ? ':' + window.location.port : '';
        const apiUrl = `${window.location.protocol}//${adresse}${port}/api`;
        const uploadUrl = `${apiUrl}/nfc-session/${this.sessionId}/upload`;
        this.qrData = JSON.stringify({ action: 'nfc_upload', url: uploadUrl });
        this.connectSse(apiUrl);
      },
      error: () => {
        this.adresseNonConfiguree = true;
      }
    });
  }

  connectSse(apiUrl: string) {
    const streamUrl = `${apiUrl}/nfc-session/${this.sessionId}/stream`;
    this.eventSource = new EventSource(streamUrl);

    this.eventSource.addEventListener('INIT', (event) => {
      console.log('SSE Connecté:', event);
    });

    this.eventSource.addEventListener('NFC_DATA', (event: MessageEvent) => {
      console.log('Données NFC reçues du mobile !');
      try {
        const data = JSON.parse(event.data);
        this.successData = data;

        // Fermer la modale et propager les données après 2 secondes
        setTimeout(() => {
          this.nfcDataReceived.emit(data);
          this.close();
        }, 1500);
      } catch (e) {
        console.error('Erreur parsing JSON NFC', e);
      }
    });

    this.eventSource.onerror = (error) => {
      console.error('Erreur EventSource', error);
      // Optionnel : this.close()
    };
  }

  close() {
    this.closeModal.emit();
  }

  ngOnDestroy() {
    if (this.eventSource) {
      this.eventSource.close();
    }
  }
}
