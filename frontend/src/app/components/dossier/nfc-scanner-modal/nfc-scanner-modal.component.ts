import { Component, EventEmitter, Output, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { QRCodeModule } from 'angularx-qrcode';
import { v4 as uuidv4 } from 'uuid';
import { ParametresService } from '../../../services/parametres.service';
import { AuthService } from '../../../services/auth.service';
import { NfcService, NfcData } from '../../../services/nfc.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-nfc-scanner-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, QRCodeModule],
  template: `
    <div class="modal-overlay" (click)="close()">
      <div class="modal-content" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h2>📲 Scanner NFC</h2>
          <button class="close-btn" (click)="close()">✕</button>
        </div>

        <!-- Adresse réseau locale non configurée : pas de QR code à générer -->
        <div class="modal-body config-manquante" *ngIf="adresseNonConfiguree">
          <span class="material-icons warning-icon">wifi_off</span>
          <h3>Adresse réseau non configurée</h3>
          <p>
            Le téléphone ne peut pas joindre ce poste. Il faut d'abord configurer l'adresse de ce poste sur le réseau local.
          </p>
          <p *ngIf="estMaitre">
            Page <b>Paramètres</b> → « Adresse réseau locale du poste ».
          </p>
        </div>

        <div class="modal-body" *ngIf="!adresseNonConfiguree && !successData">
          <div *ngIf="isUsbAvailable">
            <span class="material-icons usb-icon" style="font-size: 4rem; color: #4ecca3;">usb</span>
            <h3>Lecteur USB Détecté</h3>
            <p class="instructions">Veuillez saisir les clés MRZ pour déverrouiller la puce (indispensable), posez la carte sur le lecteur, puis cliquez sur Lire.</p>
            
            <div class="mrz-form" style="display:flex; flex-direction:column; gap:10px; margin-bottom: 1rem; text-align: left;">
              <input type="text" [(ngModel)]="mrzDoc" placeholder="N° Document (ex: 123456789)" class="form-control" style="padding:8px; border-radius:4px; border:1px solid #4ecca3; background: #1e293b; color: white;">
              <input type="text" [(ngModel)]="mrzDob" placeholder="Date naissance (AAMMJJ)" class="form-control" style="padding:8px; border-radius:4px; border:1px solid #4ecca3; background: #1e293b; color: white;">
              <input type="text" [(ngModel)]="mrzExp" placeholder="Date expiration (AAMMJJ)" class="form-control" style="padding:8px; border-radius:4px; border:1px solid #4ecca3; background: #1e293b; color: white;">
              <button class="btn btn-primary" (click)="lireUsb()" [disabled]="isReadingUsb" style="margin-top:10px;">
                <span class="spinner" *ngIf="isReadingUsb"></span> {{ isReadingUsb ? 'Lecture en cours...' : 'Lire la puce' }}
              </button>
            </div>
            
            <div class="listening-state" *ngIf="usbError">
              <span class="material-icons" style="color:#D16D6A; vertical-align:middle;">error</span> <i style="color:#D16D6A;">{{ usbError }}</i>
            </div>
          </div>

          <div *ngIf="!isUsbAvailable">
            <p class="instructions">
              <strong>Astuce :</strong> Vous pouvez utiliser la webcam de l'ordinateur pour scanner la MRZ et éviter de le faire sur le mobile !
            </p>
            
            <div *ngIf="isWebcamActive" class="webcam-container">
              <video #webcamVideo autoplay playsinline style="width:100%; max-height:250px; object-fit:cover; border-radius:8px;"></video>
              <button class="btn btn-primary" (click)="captureMrz()" [disabled]="isScanningMrz" style="margin-top:10px; width:100%;">
                <span class="spinner" *ngIf="isScanningMrz"></span> {{ isScanningMrz ? 'Analyse OCR en cours...' : 'Prendre la photo' }}
              </button>
              <button class="btn btn-secondary" (click)="stopWebcam()" style="margin-top:5px; width:100%;">Annuler</button>
            </div>
            
            <div *ngIf="!isWebcamActive && mrzDoc" class="mrz-success-box" style="background:rgba(78,204,163,0.1); border:1px solid #4ecca3; padding:10px; border-radius:8px; margin-bottom:15px; color:#4ecca3;">
              <span class="material-icons" style="vertical-align:middle;">check_circle</span> MRZ scannée avec succès ! Le QR code est prêt.
            </div>

            <div *ngIf="!isWebcamActive" style="margin-bottom: 15px;">
              <button class="btn btn-secondary" (click)="startWebcam()" *ngIf="!mrzDoc" style="width:100%; display:flex; align-items:center; justify-content:center; gap:8px;">
                <span class="material-icons">photo_camera</span> Scanner la MRZ (Webcam)
              </button>
            </div>

            <p class="instructions" *ngIf="!isWebcamActive">
              <strong>1.</strong> Connectez votre mobile au même réseau Wi-Fi.<br>
              <strong>2.</strong> Ouvrez l'application <b>Frida Mobile</b>.<br>
              <strong>3.</strong> Scannez ce QR Code.
            </p>

            <div class="qr-container" *ngIf="qrData && !isWebcamActive">
              <qrcode [qrdata]="qrData" [width]="256" [errorCorrectionLevel]="'M'"></qrcode>
            </div>

            <div class="listening-state" *ngIf="qrData">
              <span class="spinner"></span> <i>En attente des données du mobile...</i>
            </div>
          </div>
        </div>

        <div class="modal-body success" *ngIf="successData">
          <span class="material-icons success-icon">check_circle</span>
          <h3>Lecture Réussie !</h3>
          <p>{{ successData.nom }} {{ successData.prenom }}</p>
          <p *ngIf="successData.nomArabe || successData.prenomArabe" style="font-size: 1.2rem; color: #4ecca3; font-weight: bold; font-family: 'Amiri', 'Arial', sans-serif;" dir="rtl">
            {{ successData.nomArabe }} {{ successData.prenomArabe }}
          </p>
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
      max-height: 90vh;
      overflow-y: auto;
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
  @Output() nfcDataReceived = new EventEmitter<NfcData>();

  sessionId: string = '';
  qrData: string = '';
  successData: NfcData | null = null;
  adresseNonConfiguree = false;
  estMaitre = false;
  isUsbAvailable = false;
  
  mrzDoc: string = '';
  mrzDob: string = '';
  mrzExp: string = '';
  isReadingUsb = false;
  usbError: string = '';
  
  // Webcam variables
  isWebcamActive = false;
  isScanningMrz = false;
  private videoStream: MediaStream | null = null;
  private nfcSubscription?: Subscription;

  constructor(
    private parametresService: ParametresService, 
    private authService: AuthService,
    private nfcService: NfcService
  ) {}

  ngOnInit() {
    this.sessionId = uuidv4();
    this.estMaitre = this.authService.isMaitre();

    // 1. On vérifie d'abord si un lecteur USB est branché et si l'Agent Local répond
    this.nfcService.checkUsbAgentAvailable().subscribe(isUsb => {
      this.isUsbAvailable = isUsb;
      
      if (!this.isUsbAvailable) {
        // Mode Mobile : On génère le QR code
        this.setupMobileNfc();
      }
    });
  }

  lireUsb() {
    this.usbError = '';
    
    if (!this.mrzDoc || !this.mrzDob || !this.mrzExp) {
      this.usbError = "Veuillez remplir tous les champs MRZ";
      return;
    }

    this.isReadingUsb = true;
    this.nfcService.readViaUsb(this.mrzDoc, this.mrzDob, this.mrzExp).subscribe({
      next: (data) => {
        this.isReadingUsb = false;
        this.handleSuccess(data);
      },
      error: (err) => {
        this.isReadingUsb = false;
        console.error("Erreur USB", err);
        this.usbError = err.error?.error || "Erreur de lecture (Vérifiez les clés MRZ et le placement de la carte).";
      }
    });
  }

  private setupMobileNfc() {
    this.parametresService.lire().subscribe({
      next: (p) => {
        const adresse = (p.adresseReseauLocale || '').trim();
        if (!adresse) {
          this.adresseNonConfiguree = true;
          return;
        }
        
        const port = window.location.port ? ':' + window.location.port : '';
        const apiUrl = `${window.location.protocol}//${adresse}${port}/api`;
        this.updateQrData();
        
        // Utilisation du nouveau NfcService
        this.nfcSubscription = this.nfcService.listenToMobileNfc(apiUrl, this.sessionId).subscribe({
          next: (data) => this.handleSuccess(data),
          error: (err) => console.error('Erreur NFC Mobile', err)
        });
      },
      error: () => {
        this.adresseNonConfiguree = true;
      }
    });
  }

  private handleSuccess(data: NfcData) {
    this.successData = data;
    setTimeout(() => {
      this.nfcDataReceived.emit(data);
      this.close();
    }, 1500);
  }

  close() {
    this.stopWebcam();
    this.closeModal.emit();
  }

  ngOnDestroy() {
    this.stopWebcam();
    if (this.nfcSubscription) {
      this.nfcSubscription.unsubscribe();
    }
  }

  async startWebcam() {
    this.isWebcamActive = true;
    try {
      this.videoStream = await navigator.mediaDevices.getUserMedia({ 
        video: { 
          facingMode: 'environment',
          width: { ideal: 1920 },
          height: { ideal: 1080 }
        } 
      });
      setTimeout(() => {
        const videoElement = document.querySelector('video') as HTMLVideoElement;
        if (videoElement) {
          videoElement.srcObject = this.videoStream;
        }
      }, 100);
    } catch (e) {
      console.error("Impossible d'accéder à la webcam", e);
      this.isWebcamActive = false;
    }
  }

  stopWebcam() {
    if (this.videoStream) {
      this.videoStream.getTracks().forEach(track => track.stop());
      this.videoStream = null;
    }
    this.isWebcamActive = false;
  }

  async captureMrz() {
    const video = document.querySelector('video') as HTMLVideoElement;
    if (!video) return;

    this.isScanningMrz = true;
    
    // Créer un canvas pour capturer l'image
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    const base64Image = canvas.toDataURL('image/jpeg', 0.8);

    try {
      // Appel à l'API Spring Boot
      const response = await fetch('/api/pdfs/mrz-webcam', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ image: base64Image })
      });
      
      if (!response.ok) {
        const errData = await response.json().catch(() => ({}));
        throw new Error(errData.message || "Erreur OCR côté serveur");
      }
      
      const data = await response.json();
      this.mrzDoc = data.documentNumber;
      this.mrzDob = data.dateOfBirth;
      this.mrzExp = data.dateOfExpiry;
      
      this.stopWebcam();
      this.updateQrData();
      
    } catch (e: any) {
      console.error(e);
      alert("Impossible de lire la MRZ : " + (e.message || "Veuillez reprendre la photo. Assurez-vous que l'image est nette, sans reflet, et que la zone MRZ est bien visible."));
    } finally {
      this.isScanningMrz = false;
    }
  }

  private updateQrData() {
    this.parametresService.lire().subscribe(p => {
      const adresse = (p.adresseReseauLocale || '').trim();
      if (!adresse) return;
      
      const port = window.location.port ? ':' + window.location.port : '';
      const apiUrl = `${window.location.protocol}//${adresse}${port}/api`;
      const uploadUrl = `${apiUrl}/nfc-session/${this.sessionId}/upload`;
      
      const payload: any = { action: 'nfc_upload', url: uploadUrl };
      
      if (this.mrzDoc && this.mrzDob && this.mrzExp) {
        payload.mrz = { doc: this.mrzDoc, dob: this.mrzDob, exp: this.mrzExp };
      }
      
      this.qrData = JSON.stringify(payload);
    });
  }
}
