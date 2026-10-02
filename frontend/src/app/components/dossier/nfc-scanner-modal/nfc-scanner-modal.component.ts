import { Component, EventEmitter, Output, Input, OnInit, OnDestroy, ElementRef, Renderer2 } from '@angular/core';
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
          <h2>{{ mode === 'usb' ? '🪪 Scanner via USB' : '📱 Scanner via Mobile' }}</h2>
          <button class="close-btn" (click)="close()">✕</button>
        </div>

        <div class="modal-body config-manquante" *ngIf="mode === 'mobile' && adresseNonConfiguree">
          <span class="material-icons warning-icon">wifi_off</span>
          <h3>Adresse réseau non configurée</h3>
          <p>Le téléphone ne peut pas joindre ce poste. Configurez l'adresse dans les paramètres.</p>
        </div>

        <div class="modal-body" *ngIf="(!adresseNonConfiguree || mode === 'usb') && !successData">
          <div *ngIf="mode === 'usb'">
            <div *ngIf="!isUsbAvailable && !checkingUsb" style="color:#D16D6A; margin-bottom: 1rem;">
              <span class="material-icons" style="font-size:3rem; display:block; margin-bottom:10px;">error</span>
              Agent Local non détecté. Assurez-vous d'avoir lancé le programme "Frida USB Agent" sur votre PC.
            </div>

            <div *ngIf="isUsbAvailable">
              <span class="material-icons usb-icon" style="font-size: 3rem; color: #4ecca3; margin-bottom: 10px; display: block;">usb</span>
              <div *ngIf="!mrzDoc || !mrzDob || !mrzExp || forceManualEntry">
                <p class="instructions" style="text-align:center;">Veuillez saisir les 3 informations de la zone MRZ pour déverrouiller la puce.</p>
                <div class="manual-entry-container" style="display: flex; flex-direction: column; gap: 10px; margin-bottom: 15px;">
                  <input type="text" [(ngModel)]="mrzDoc" placeholder="N° Document (ex: 123456789)" class="form-control" style="padding: 10px; border-radius: 6px; border: 1px solid rgba(255,255,255,0.2); background: rgba(0,0,0,0.2); color: white;">
                  <input type="text" [(ngModel)]="mrzDob" placeholder="Date Naissance (AAMMJJ)" class="form-control" style="padding: 10px; border-radius: 6px; border: 1px solid rgba(255,255,255,0.2); background: rgba(0,0,0,0.2); color: white;">
                  <input type="text" [(ngModel)]="mrzExp" placeholder="Date Expiration (AAMMJJ)" class="form-control" style="padding: 10px; border-radius: 6px; border: 1px solid rgba(255,255,255,0.2); background: rgba(0,0,0,0.2); color: white;">
                </div>
                <button class="btn btn-primary" (click)="forceManualEntry = false" [disabled]="!mrzDoc || !mrzDob || !mrzExp" style="margin-top:10px; width:100%;">
                  Valider les clés
                </button>
              </div>

              <div *ngIf="mrzDoc && mrzDob && mrzExp && !forceManualEntry">
                <div class="mrz-success-box" style="background:rgba(78,204,163,0.1); border:1px solid #4ecca3; padding:10px; border-radius:8px; margin-bottom:15px; color:#4ecca3; text-align: left; position: relative;">
                  <span class="material-icons" style="vertical-align:middle;">check_circle</span> Clés validées !<br>
                  <small>{{ mrzDoc }} / {{ mrzDob }} / {{ mrzExp }}</small>
                  <button (click)="forceManualEntry = true" style="position: absolute; right: 10px; top: 10px; background: none; border: none; color: #4ecca3; cursor: pointer;"><span class="material-icons" style="font-size: 1rem;">edit</span></button>
                </div>
                <p class="instructions" style="text-align:center;">Posez maintenant la carte d'identité sur le <b>lecteur uTrust</b>.</p>
                <button class="btn btn-primary" (click)="lireUsb()" [disabled]="isReadingUsb" style="margin-top:10px; width: 100%; font-size: 1.1rem; padding: 12px;">
                  <span class="spinner" *ngIf="isReadingUsb"></span> {{ isReadingUsb ? 'Lecture sans contact en cours...' : '💳 Lire la puce' }}
                </button>
                <div class="listening-state" *ngIf="usbError" style="margin-top: 15px;">
                  <span class="material-icons" style="color:#D16D6A; vertical-align:middle;">error</span> <i style="color:#D16D6A;">{{ usbError }}</i>
                </div>
              </div>
            </div>
          </div>

          <div *ngIf="mode === 'mobile' && !adresseNonConfiguree">
            <p class="instructions">
              <strong>1.</strong> Connectez votre mobile au même réseau Wi-Fi.<br>
              <strong>2.</strong> Ouvrez l'application <b>Frida Mobile</b>.<br>
              <strong>3.</strong> Scannez ce QR Code avec l'application.
            </p>
            <div class="qr-container" *ngIf="qrData">
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
    .modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0, 0, 0, 0.7); backdrop-filter: blur(5px); display: flex; align-items: center; justify-content: center; z-index: 10000; }
    .modal-content { background: #1e293b; border: 1px solid rgba(78, 204, 163, 0.3); border-radius: 12px; width: 400px; max-width: 90vw; box-shadow: 0 10px 25px rgba(0,0,0,0.5); color: white; max-height: 90vh; overflow-y: auto; }
    .modal-header { display: flex; justify-content: space-between; align-items: center; padding: 1rem 1.5rem; border-bottom: 1px solid rgba(255,255,255,0.1); }
    .modal-header h2 { margin: 0; font-size: 1.25rem; color: #4ecca3; }
    .close-btn { background: none; border: none; color: white; font-size: 1.5rem; cursor: pointer; opacity: 0.7; }
    .close-btn:hover { opacity: 1; }
    .modal-body { padding: 1.5rem; text-align: center; }
    .instructions { text-align: left; background: rgba(255,255,255,0.05); padding: 1rem; border-radius: 8px; margin-bottom: 1rem; line-height: 1.6; font-size: 0.95rem; }
    .qr-container { background: white; padding: 1rem; border-radius: 8px; display: inline-block; margin-bottom: 1rem; }
    .listening-state { color: #ffb84d; margin-top: 1rem; font-size: 0.9rem; }
    .success { text-align: center; padding: 2rem; }
    .success-icon { font-size: 4rem; color: #4ecca3; margin-bottom: 1rem; }
    .config-manquante { text-align: left; }
    .warning-icon { font-size: 3rem; color: #ffb84d; display: block; text-align: center; margin-bottom: 0.5rem; }
    .spinner { display: inline-block; width: 16px; height: 16px; border: 2px solid rgba(255,255,255,0.3); border-top-color: white; border-radius: 50%; animation: spin 1s linear infinite; vertical-align: middle; margin-right: 8px; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class NfcScannerModalComponent implements OnInit, OnDestroy {
  @Input() mode: 'mobile' | 'usb' = 'mobile';
  @Output() closeModal = new EventEmitter<void>();
  @Output() nfcDataReceived = new EventEmitter<NfcData>();

  sessionId: string = '';
  qrData: string = '';
  successData: NfcData | null = null;
  adresseNonConfiguree = false;
  estMaitre = false;
  forceManualEntry = false;
  
  checkingUsb = false;
  isUsbAvailable = false;
  mrzDoc: string = '';
  mrzDob: string = '';
  mrzExp: string = '';
  isReadingUsb = false;
  usbError: string = '';
  isScanningMrz = false;
  private videoStream: MediaStream | null = null;
  
  private nfcSubscription?: Subscription;

  constructor(
    private parametresService: ParametresService, 
    private authService: AuthService,
    private nfcService: NfcService,
    private el: ElementRef,
    private renderer: Renderer2
  ) {}

  ngOnInit() {
    this.renderer.appendChild(document.body, this.el.nativeElement);
    this.sessionId = uuidv4();
    this.estMaitre = this.authService.isMaitre();

    if (this.mode === 'usb') {
      this.checkingUsb = true;
      this.nfcService.checkUsbAgentAvailable().subscribe(isUsb => {
        this.checkingUsb = false;
        this.isUsbAvailable = isUsb;
        if (this.isUsbAvailable) {
          this.startWebcam();
        }
      });
    } else {
      this.setupMobileNfc();
    }
  }

  ngOnDestroy() {
    this.stopWebcam();
    if (this.nfcSubscription) this.nfcSubscription.unsubscribe();
    if (this.el.nativeElement && this.el.nativeElement.parentNode === document.body) {
      this.renderer.removeChild(document.body, this.el.nativeElement);
    }
  }

  close() {
    this.closeModal.emit();
  }

  async startWebcam() {
    try {
      this.videoStream = await navigator.mediaDevices.getUserMedia({ 
        video: { facingMode: 'environment', width: { ideal: 1920 }, height: { ideal: 1080 } } 
      });
      setTimeout(() => {
        const videoElement = document.querySelector('video') as HTMLVideoElement;
        if (videoElement) {
          videoElement.srcObject = this.videoStream;
        }
      }, 100);
    } catch (e) {
      console.error("Impossible d'accéder à la webcam", e);
      this.usbError = "Impossible d'accéder à la webcam de votre ordinateur.";
    }
  }

  stopWebcam() {
    if (this.videoStream) {
      this.videoStream.getTracks().forEach(track => track.stop());
      this.videoStream = null;
    }
  }

  async captureMrz() {
    const video = document.querySelector('video') as HTMLVideoElement;
    if (!video) return;

    this.isScanningMrz = true;
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    
    ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
    const base64Image = canvas.toDataURL('image/jpeg', 0.8);

    try {
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
      
    } catch (e: any) {
      console.error(e);
      alert("Impossible de lire la MRZ : " + (e.message || "Veuillez reprendre la photo (image nette, sans reflet)."));
    } finally {
      this.isScanningMrz = false;
    }
  }

  lireUsb() {
    this.usbError = '';
    if (!this.mrzDoc || !this.mrzDob || !this.mrzExp) {
      this.usbError = "Clés MRZ manquantes";
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
        this.usbError = err.error?.error || "Erreur de lecture de la puce. Assurez-vous que la carte est bien posée.";
      }
    });
  }

  private setupMobileNfc() {
    this.parametresService.lire().subscribe({
      next: (p) => {
        let adresse = (p.adresseReseauLocale || '').trim();
        
        // Auto-détection si on accède déjà via une IP réseau
        if (window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1') {
            adresse = window.location.hostname;
        }

        if (!adresse) {
          this.adresseNonConfiguree = true;
          return;
        }
        
        const port = window.location.port ? ':' + window.location.port : '';
        const apiUrl = window.location.protocol + '//' + adresse + port + '/api';
        
        const uploadUrl = `${apiUrl}/nfc-session/${this.sessionId}/upload`;
        const payload: any = { action: 'nfc_upload', url: uploadUrl };
        if (this.mrzDoc && this.mrzDob && this.mrzExp) {
          payload.mrz = { doc: this.mrzDoc, dob: this.mrzDob, exp: this.mrzExp };
        }
        
        this.qrData = JSON.stringify(payload);
        
        this.nfcSubscription = this.nfcService.listenToMobileNfc(apiUrl, this.sessionId).subscribe({
          next: (event: any) => {
            if (event.type === 'MRZ_DATA') {
              const mrz = event.payload;
              if (mrz.documentNumber) this.mrzDoc = mrz.documentNumber;
              
              const formatDate = (d: any) => {
                if (typeof d === 'string' && d.length === 6 && !d.includes('-')) {
                  return d; // Déjà au format YYMMDD envoyé par l'app Flutter
                }
                if (Array.isArray(d)) {
                  // [year, month, day]
                  const y = String(d[0]).substring(2);
                  const m = String(d[1]).padStart(2, '0');
                  const day = String(d[2]).padStart(2, '0');
                  return y + m + day;
                } else if (typeof d === 'string') {
                  const parts = d.split('-');
                  if (parts.length === 3) {
                    return parts[0].substring(2) + parts[1] + parts[2];
                  }
                }
                return '';
              };
              
              if (mrz.dateOfBirth) {
                this.mrzDob = formatDate(mrz.dateOfBirth);
              }
              if (mrz.expiryDate) {
                this.mrzExp = formatDate(mrz.expiryDate);
              }
              
              // Switch to USB mode automatically and trigger read
              this.mode = 'usb';
              this.forceManualEntry = true;
              
              this.checkingUsb = true;
              this.nfcService.checkUsbAgentAvailable().subscribe(isUsb => {
                  this.checkingUsb = false;
                  this.isUsbAvailable = isUsb;
                  // On ne lance plus la lecture automatique. On attend que l'utilisateur clique.
              });
            } else if (event.type === 'NFC_DATA') {
              this.handleSuccess(event.payload);
            }
          },
          error: (err) => console.error('Erreur NFC Mobile', err)
        });
      },
      error: () => this.adresseNonConfiguree = true
    });
  }

  private handleSuccess(data: NfcData) {
    this.successData = data;
    setTimeout(() => {
      this.nfcDataReceived.emit(data);
      this.closeModal.emit();
    }, 2000);
  }
}
