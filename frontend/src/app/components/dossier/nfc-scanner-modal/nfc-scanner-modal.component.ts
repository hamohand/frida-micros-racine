import { Component, EventEmitter, Output, Input, OnInit, OnDestroy, ElementRef, Renderer2, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { QRCodeModule } from 'angularx-qrcode';
import { v4 as uuidv4 } from 'uuid';
import { ParametresService } from '../../../services/parametres.service';
import { AuthService } from '../../../services/auth.service';
import {
  NfcService,
  NfcData,
  UsbReaderInfo,
  MobileE2eeSessionInfo
} from '../../../services/nfc.service';
import { Subscription, forkJoin, of, catchError } from 'rxjs';

@Component({
  selector: 'app-nfc-scanner-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, QRCodeModule],
  template: `
    <div class="modal-overlay" (click)="onOverlayClick()">
      <div class="modal-content" (click)="$event.stopPropagation()">
        <!-- En-tête style dz-eid Standard (Sans Preuve de vie) -->
        <div class="modal-header">
          <div class="brand">
            <div class="flag" aria-hidden="true"></div>
            <div>
              <h2>
                {{ mode === 'usb'
                  ? '🖥️ dz-eid · Lecture CNIe via Lecteur USB (Sans Preuve de vie)'
                  : '📱 dz-eid · Lecture CNIe via Mobile NFC (E2EE — Sans Preuve de vie)' }}
              </h2>
              <div class="sub">
                {{ mode === 'usb'
                  ? 'Agent local PC/SC · Vérification cryptographique CSCA & Anti-clonage'
                  : 'Chiffrement de bout en bout ECDH P-256 + AES-256-GCM · Zéro preuve de vie' }}
              </div>
            </div>
          </div>

          <div class="header-right">
            <!-- Pastilles d'état temps réel (Mode USB) -->
            <div class="pills" *ngIf="mode === 'usb'">
              <span class="pill">
                <span class="dot" [ngClass]="agentDotState"></span>
                <span>{{ agentStatusText }}</span>
              </span>
              <span class="pill" [title]="readersTooltip">
                <span class="dot" [ngClass]="readerDotState"></span>
                <span>{{ readerStatusText }}</span>
              </span>
              <span class="pill">
                <span class="dot" [ngClass]="cardDotState"></span>
                <span>{{ cardStatusText }}</span>
              </span>
            </div>

            <!-- Pastilles d'état temps réel (Mode Mobile E2EE) -->
            <div class="pills" *ngIf="mode === 'mobile'">
              <span class="pill">
                <span class="dot" [ngClass]="mobileRelayDotState"></span>
                <span>{{ mobileRelayStatusText }}</span>
              </span>
              <span class="pill">
                <span class="dot ok"></span>
                <span>Sans Preuve de vie</span>
              </span>
            </div>

            <button class="close-btn" (click)="close()" title="Fermer">✕</button>
          </div>
        </div>

        <!-- Grille principale 2 colonnes (Acquisition à gauche, Fiche d'identité à droite) -->
        <div class="modal-main">
          <!-- ==================== COLONNE GAUCHE ==================== -->
          <section class="panel-card">
            <!-- ---------- MODE LECTEUR USB ---------- -->
            <ng-container *ngIf="mode === 'usb'">
              <h3>1. Clé d'accès (MRZ)</h3>

              <div class="mrz-header-row">
                <label for="mrzRawInput" style="margin: 0;">Coller la MRZ complète (facultatif)</label>
                <button type="button" class="btn-douchette" (click)="toggleUsbMrzRelay()">
                  📱 {{ showUsbQrBox ? 'Masquer la douchette' : 'Scanner la MRZ avec la douchette' }}
                </button>
              </div>

              <textarea
                id="mrzRawInput"
                [(ngModel)]="mrzRaw"
                (input)="onMrzRawInput()"
                placeholder="IDDZA1234567890<<<…&#10;8503127F3105208DZA<<<…&#10;NOM<<PRENOM<<<…"
                spellcheck="false"
              ></textarea>

              <!-- Boîte QR Code Douchette Mobile (via dz-eid-agent /v1/relay port 8989) -->
              <div class="qr-douchette-box" *ngIf="showUsbQrBox">
                <p style="font-size: 12.5px; margin: 0 0 10px; color: #e2e8f0;">
                  Scannez ce QR Code avec <b>dz-eid Douchette</b> (ou Frida Mobile) pour lire la MRZ :
                </p>

                <div class="qr-white-box" *ngIf="usbRelaySvgUrl">
                  <img [src]="usbRelaySvgUrl" alt="QR Code Douchette MRZ" style="width: 155px; height: 155px; display: block;" />
                </div>
                <div class="qr-white-box" *ngIf="!usbRelaySvgUrl && usbFallbackQrData">
                  <qrcode [qrdata]="usbFallbackQrData" [width]="155" [errorCorrectionLevel]="'M'"></qrcode>
                </div>

                <div *ngIf="usbRelayAddresses.length > 1" style="margin-top: 8px;">
                  <label style="font-size: 11px; margin: 0 0 4px;">Adresse réseau du poste :</label>
                  <select class="host-select" [(ngModel)]="usbRelaySelectedHost" (change)="onUsbRelayHostChange()">
                    <option *ngFor="let addr of usbRelayAddresses" [value]="addr">{{ addr }}</option>
                  </select>
                </div>

                <p class="qr-status" [class.err]="usbRelayError">
                  {{ usbRelayStatusText }}
                </p>
              </div>

              <div class="hint">Les 3 champs ci-dessous se remplissent automatiquement ou peuvent être saisis librement.</div>

              <label for="docInput">N° du document</label>
              <input
                type="text"
                id="docInput"
                [(ngModel)]="mrzDoc"
                (keyup.enter)="lireUsb()"
                maxlength="22"
                placeholder="Ex: 123456789"
                autocomplete="off"
                spellcheck="false"
              />

              <div class="row-2">
                <div>
                  <label for="dobInput">Naissance (AAMMJJ)</label>
                  <input
                    type="text"
                    id="dobInput"
                    [(ngModel)]="mrzDob"
                    (keyup.enter)="lireUsb()"
                    maxlength="6"
                    inputmode="numeric"
                    placeholder="AAMMJJ"
                    autocomplete="off"
                  />
                </div>
                <div>
                  <label for="doeInput">Expiration (AAMMJJ)</label>
                  <input
                    type="text"
                    id="doeInput"
                    [(ngModel)]="mrzExp"
                    (keyup.enter)="lireUsb()"
                    maxlength="6"
                    inputmode="numeric"
                    placeholder="AAMMJJ"
                    autocomplete="off"
                  />
                </div>
              </div>

              <h3 style="margin-top: 18px;">2. Poser la carte sur le lecteur USB</h3>
              <button
                type="button"
                class="btn-primary-dzeid"
                (click)="lireUsb()"
                [disabled]="isReadingUsb || !clesSaisiesValides"
              >
                <span class="spinner" *ngIf="isReadingUsb"></span>
                {{ isReadingUsb ? 'Lecture sans contact en cours…' : '💳 Lire la carte' }}
              </button>

              <!-- Barre de progression temps réel (WS PROGRESS) -->
              <div class="progress-box" *ngIf="showProgress">
                <div class="progress-bar">
                  <div class="progress-fill" [style.width.%]="progressPct"></div>
                </div>
                <p>{{ progressText }}</p>
              </div>

              <!-- Alerte d'erreur -->
              <div class="alert-err" *ngIf="usbError">
                {{ usbError }}
              </div>

              <!-- Journal des événements de l'agent -->
              <div class="event-log" *ngIf="logs.length > 0">
                <div *ngFor="let line of logs">{{ line }}</div>
              </div>
            </ng-container>

            <!-- ---------- MODE MOBILE NFC (E2EE) ---------- -->
            <ng-container *ngIf="mode === 'mobile'">
              <h3>1. Scanner avec frida-mobile-nfc ou dz-eid Mobile</h3>
              <p class="hint" style="margin-bottom: 12px;">
                Ouvrez <b>frida-mobile-nfc</b> (ou <b>dz-eid Mobile Standard</b>) sur votre smartphone, scannez ce QR Code puis lisez votre carte NFC.<br />
                L'identité est transmise directement <b>sans étape de preuve de vie</b>.
              </p>

              <div class="qr-mobile-container">
                <div *ngIf="!qrData && !adresseNonConfiguree" style="padding: 40px 10px; color: #94a3b8;">
                  <span class="spinner"></span> Génération de la clé ECDH P-256 et du QR Code…
                </div>

                <div *ngIf="adresseNonConfiguree" style="padding: 20px; color: #f59e0b;">
                  ⚠️ Aucune adresse réseau locale détectée. Renseignez l'adresse IPv4 de votre PC ci-dessous (ex: <code>192.168.1.3</code>).
                </div>

                <div *ngIf="qrData" class="qr-white-box" style="margin: 0 auto;">
                  <qrcode [qrdata]="qrData" [width]="240" [errorCorrectionLevel]="'L'"></qrcode>
                </div>

                <div class="relay-url" *ngIf="mobileRelayUrl">
                  Passerelle mobile : <code>{{ mobileRelayUrl }}</code>
                </div>

                <div style="margin-top: 10px; text-align: left;">
                  <label style="font-size: 11px; margin: 0 0 4px;">Adresse IPv4 du PC (Wi-Fi / LAN) :</label>
                  <div style="display: flex; gap: 6px; align-items: center;">
                    <input
                      type="text"
                      [(ngModel)]="selectedMobileHost"
                      (keyup.enter)="onMobileHostChange()"
                      placeholder="Ex: 192.168.1.3"
                      style="flex: 1; padding: 7px 10px; font-size: 13px;"
                    />
                    <button
                      type="button"
                      class="btn-douchette"
                      style="padding: 7px 12px; font-size: 12px; white-space: nowrap;"
                      (click)="onMobileHostChange()"
                    >
                      Appliquer
                    </button>
                  </div>
                  <select
                    *ngIf="mobileAddresses.length > 1"
                    class="host-select"
                    style="margin-top: 6px; font-size: 12px; padding: 6px 8px;"
                    [(ngModel)]="selectedMobileHost"
                    (change)="onMobileHostChange()"
                  >
                    <option *ngFor="let ip of mobileAddresses" [value]="ip">{{ ip }}</option>
                  </select>
                </div>
              </div>

              <div class="mobile-loader" [class.ok]="!!successData" [class.err]="!!mobileError">
                {{ mobileLoaderText }}
              </div>

              <div style="margin-top: 12px;">
                <button type="button" class="btn-secondary-dzeid" style="width: 100%;" (click)="restartMobileSession()">
                  🔄 Nouveau QR Code (Nouvelle lecture)
                </button>
              </div>
            </ng-container>
          </section>

          <!-- ==================== COLONNE DROITE : FICHE D'IDENTITÉ CERTIFIÉE ==================== -->
          <section class="panel-card">
            <h3>2. Identité certifiée & Intégration Frida (Données exigées par Frida)</h3>

            <!-- État vide avant lecture -->
            <div class="empty-state" *ngIf="!successData">
              <div style="font-size: 46px; margin-bottom: 10px;">🪪</div>
              <div style="font-size: 15px; font-weight: 600; color: #e2e8f0; margin-bottom: 6px;">
                Le résultat de la lecture s'affichera ici.
              </div>
              <div class="hint">
                Lecture rapide des seules données exigées par Frida (état civil arabe & latin, NIN, dates, autorité, SOD & anti-clonage — sans photo ni preuve de vie).
              </div>
            </div>

            <!-- Fiche d'identité reçue -->
            <div class="identity-card" *ngIf="successData">
              <!-- Barre d'action principale Frida -->
              <div class="frida-action-bar">
                <button type="button" class="btn-confirm-frida" (click)="confirmerEtAjouter()">
                  ✅ Utiliser cette identité dans Frida
                </button>
                <button type="button" class="btn-secondary-dzeid" (click)="reinitialiserLecture()">
                  🔄 Lire une autre carte
                </button>
              </div>

              <div class="id-header">
                <div class="names">
                  <p class="name-lat">
                    {{ (successData.latines || '') + ' ' + (successData.prenomLatines || '') }}
                  </p>
                  <p class="name-ar" *ngIf="successData.nomArabe || successData.prenomArabe" dir="rtl">
                    {{ (successData.nomArabe || '') + ' ' + (successData.prenomArabe || '') }}
                  </p>
                  <span class="nin-badge">
                    {{ successData.nin ? 'NIN ' + successData.nin : 'NIN non disponible' }}
                  </span>

                  <!-- Badges cryptographiques de la puce (sans liveness) -->
                  <div class="badges" *ngIf="successData.verification">
                    <span class="badge VALID" *ngIf="successData.verification.accessMethod">
                      Accès {{ successData.verification.accessMethod }}
                    </span>
                    <span
                      class="badge"
                      *ngIf="successData.verification.passiveAuthentication?.dataIntegrity as di"
                      [ngClass]="di"
                    >
                      Intégrité : {{ frStatus(di) }}
                    </span>
                    <span
                      class="badge"
                      *ngIf="successData.verification.passiveAuthentication?.signature as sig"
                      [ngClass]="sig"
                    >
                      Signature SOD : {{ frStatus(sig) }}
                    </span>
                    <span
                      class="badge"
                      *ngIf="successData.verification.passiveAuthentication?.certificateChain as csca"
                      [ngClass]="csca"
                    >
                      Chaîne CSCA : {{ frStatus(csca) }}
                    </span>
                    <span
                      class="badge"
                      *ngIf="successData.verification.activeAuthentication?.result as aa"
                      [ngClass]="aa"
                    >
                      Anti-clonage : {{ frStatus(aa) }}
                    </span>
                  </div>

                  <div
                    class="summary-line"
                    *ngIf="successData.verification?.passiveAuthentication?.summary as paSum"
                    [class.bad]="paSum.startsWith('ALERTE')"
                  >
                    {{ paSum }}
                  </div>
                  <div
                    class="summary-line"
                    *ngIf="successData.verification?.activeAuthentication?.summary as aaSum"
                    [class.bad]="successData.verification?.activeAuthentication?.result === 'INVALID'"
                  >
                    {{ aaSum }}
                  </div>
                </div>
              </div>

              <!-- Grille des champs détaillés bilingues exigés par Frida -->
              <div class="fields-grid">
                <div class="field-item">
                  <div class="k">Date de naissance</div>
                  <div class="v">{{ fmtDate(successData.dateNaissance) }}</div>
                </div>
                <div class="field-item">
                  <div class="k">Lieu de naissance</div>
                  <div class="v">
                    <span>{{ successData.lieuNaissanceLatin || '—' }}</span>
                    <span class="ar" *ngIf="successData.lieuNaissanceArabe">{{ successData.lieuNaissanceArabe }}</span>
                  </div>
                </div>
                <div class="field-item">
                  <div class="k">Sexe</div>
                  <div class="v">
                    <span>{{ successData.sexe === 'M' ? 'Masculin' : successData.sexe === 'F' ? 'Féminin' : (successData.sexe || '—') }}</span>
                    <span class="ar" *ngIf="successData.sexeArabe">{{ successData.sexeArabe }}</span>
                  </div>
                </div>
                <div class="field-item">
                  <div class="k">Nationalité</div>
                  <div class="v">{{ successData.nationalite || '—' }}</div>
                </div>
                <div class="field-item">
                  <div class="k">N° du document</div>
                  <div class="v">{{ successData.documentNumber || '—' }}</div>
                </div>
                <div class="field-item">
                  <div class="k">Délivré le</div>
                  <div class="v">{{ fmtDate(successData.delivreLe) }}</div>
                </div>
                <div class="field-item">
                  <div class="k">Expire le</div>
                  <div class="v">
                    {{ fmtDate(successData.expireLe) }}
                    <b *ngIf="isExpired(successData.expireLe)" style="color: #ef4444; margin-left: 6px;">(expiré)</b>
                  </div>
                </div>
                <div class="field-item">
                  <div class="k">Autorité</div>
                  <div class="v">
                    <span>{{ successData.delivreParLatin || '—' }}</span>
                    <span class="ar" *ngIf="successData.delivreParArabe">{{ successData.delivreParArabe }}</span>
                  </div>
                </div>
              </div>

              <!-- Avertissements éventuels -->
              <div class="warnings-box" *ngIf="allWarnings.length > 0">
                <div *ngFor="let w of allWarnings">⚠ {{ w }}</div>
              </div>

              <!-- JSON complet pliable -->
              <details style="margin-top: 14px;">
                <summary style="cursor: pointer; font-size: 12px; color: #94a3b8;">
                  Voir la réponse JSON complète (contrat IdentityRecord v1.1)
                </summary>
                <div style="margin: 8px 0;">
                  <button type="button" class="btn-secondary-dzeid" style="padding: 4px 10px; font-size: 11px;" (click)="copyJson()">
                    {{ jsonCopied ? '✅ JSON copié !' : '📋 Copier le JSON' }}
                  </button>
                </div>
                <pre class="json-pre">{{ truncatedJsonDisplay }}</pre>
              </details>
            </div>
          </section>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .modal-overlay {
      position: fixed; top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(8, 13, 26, 0.82); backdrop-filter: blur(6px);
      display: flex; align-items: center; justify-content: center; z-index: 10000;
      padding: 16px;
    }
    .modal-content {
      background: #0f172a; border: 1px solid #243252; border-radius: 16px;
      width: 1120px; max-width: 96vw; max-height: 92vh; overflow-y: auto;
      box-shadow: 0 20px 50px rgba(0, 0, 0, 0.65); color: #e6edf7;
      font-family: "Segoe UI", system-ui, -apple-system, sans-serif;
    }
    .modal-header {
      display: flex; justify-content: space-between; align-items: center;
      padding: 14px 22px; border-bottom: 1px solid #243252;
      background: rgba(17, 26, 46, 0.9); flex-wrap: wrap; gap: 12px;
    }
    .brand { display: flex; align-items: center; gap: 12px; }
    .flag {
      width: 32px; height: 22px; border-radius: 4px;
      background: linear-gradient(90deg, #006233 50%, #ffffff 50%);
      position: relative; box-shadow: 0 0 0 1px rgba(255,255,255,.15); flex-shrink: 0;
    }
    .flag::after {
      content: "☪"; color: #d21034; font-size: 13px;
      position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
    }
    .modal-header h2 { margin: 0; font-size: 16px; font-weight: 600; color: #e6edf7; }
    .sub { font-size: 11.5px; color: #93a4c3; margin-top: 2px; }
    .header-right { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
    .pills { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
    .pill {
      display: inline-flex; align-items: center; gap: 7px;
      padding: 5px 11px; border-radius: 999px; font-size: 12px;
      background: #17233d; border: 1px solid #243252; color: #e6edf7;
    }
    .dot { width: 8px; height: 8px; border-radius: 50%; background: #64748b; }
    .dot.ok { background: #10b981; box-shadow: 0 0 8px #10b981; }
    .dot.wait { background: #f59e0b; }
    .dot.ko { background: #ef4444; }
    .close-btn {
      background: none; border: none; color: #94a3b8; font-size: 1.4rem;
      cursor: pointer; padding: 4px 8px; border-radius: 6px;
    }
    .close-btn:hover { color: #fff; background: rgba(255,255,255,0.08); }

    .modal-main {
      display: grid; grid-template-columns: 380px 1fr; gap: 20px; padding: 20px;
    }
    @media (max-width: 920px) {
      .modal-main { grid-template-columns: 1fr; }
    }
    .panel-card {
      background: #111a2e; border: 1px solid #243252; border-radius: 14px;
      padding: 18px; box-shadow: 0 8px 24px rgba(0,0,0,0.25);
    }
    .panel-card h3 {
      margin: 0 0 12px 0; font-size: 13px; text-transform: uppercase;
      letter-spacing: .06em; color: #10b981;
    }
    label {
      display: block; font-size: 11px; font-weight: 600; color: #93a4c3;
      margin: 10px 0 4px; text-transform: uppercase; letter-spacing: .04em;
    }
    input[type=text], textarea, .host-select {
      width: 100%; padding: 9px 11px; border: 1px solid #243252; border-radius: 8px;
      background: #0b1220; color: #e6edf7; font-size: 14px;
      font-family: Consolas, "Courier New", monospace;
    }
    textarea { height: 72px; resize: vertical; font-size: 12px; }
    input:focus, textarea:focus, .host-select:focus {
      outline: 2px solid #10b981; border-color: transparent;
    }
    .mrz-header-row {
      display: flex; justify-content: space-between; align-items: center;
      gap: 6px; flex-wrap: wrap; margin-bottom: 6px;
    }
    .btn-douchette {
      background: rgba(16, 185, 129, 0.12); border: 1px solid #10b981;
      color: #6ee7b7; padding: 4px 10px; border-radius: 6px;
      cursor: pointer; font-size: 11px; font-weight: 600;
    }
    .btn-douchette:hover { background: rgba(16, 185, 129, 0.25); }
    .qr-douchette-box {
      text-align: center; margin: 10px 0; padding: 12px;
      background: #17233d; border-radius: 10px; border: 1px dashed #10b981;
    }
    .qr-white-box {
      background: #ffffff; padding: 8px; border-radius: 8px;
      display: inline-block;
    }
    .qr-status { font-size: 12px; color: #f59e0b; margin: 8px 0 0; }
    .qr-status.err { color: #ef4444; }
    .hint { font-size: 12px; color: #93a4c3; margin-top: 6px; line-height: 1.45; }
    .row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
    .checks { display: flex; gap: 14px; margin-top: 12px; flex-wrap: wrap; }
    .checks label {
      display: flex; align-items: center; gap: 6px; margin: 0;
      text-transform: none; font-size: 12.5px; color: #cbd5e1; cursor: pointer;
    }
    .btn-primary-dzeid {
      width: 100%; margin-top: 10px; padding: 12px; border: 0; border-radius: 10px;
      background: linear-gradient(180deg, #10b981, #059669); color: #042f2e;
      font-size: 15px; font-weight: 700; cursor: pointer; transition: opacity .15s;
    }
    .btn-primary-dzeid:disabled { background: #334155; color: #94a3b8; cursor: not-allowed; }
    .btn-secondary-dzeid {
      padding: 9px 14px; border-radius: 9px; border: 1px solid #243252;
      background: #17233d; color: #e6edf7; font-size: 13px; font-weight: 600; cursor: pointer;
    }
    .btn-secondary-dzeid:hover { border-color: #10b981; }
    .progress-box { margin-top: 14px; }
    .progress-bar { height: 8px; background: #1e293b; border-radius: 999px; overflow: hidden; }
    .progress-fill { height: 100%; background: linear-gradient(90deg, #047857, #10b981); transition: width .25s; }
    .progress-box p { margin: 6px 0 0; font-size: 12px; color: #93a4c3; }
    .alert-err {
      margin-top: 12px; padding: 10px 12px; border-radius: 8px; font-size: 13px;
      background: rgba(239, 68, 68, 0.14); color: #fca5a5; border: 1px solid rgba(239, 68, 68, 0.4);
    }
    .event-log {
      margin-top: 14px; font-family: Consolas, monospace; font-size: 11px; color: #93a4c3;
      max-height: 100px; overflow: auto; background: #0b1220; border: 1px solid #243252;
      border-radius: 8px; padding: 8px 10px;
    }

    /* Mode Mobile QR */
    .qr-mobile-container {
      background: #17233d; border: 1px solid #243252; border-radius: 12px;
      padding: 16px; text-align: center;
    }
    .relay-url { font-size: 11px; color: #6ee7b7; margin-top: 8px; word-break: break-all; }
    .mobile-loader {
      margin-top: 12px; font-size: 13px; color: #fcd34d; padding: 10px;
      border-radius: 8px; background: rgba(245, 158, 11, 0.1); border: 1px solid rgba(245, 158, 11, 0.3);
      text-align: center;
    }
    .mobile-loader.ok {
      color: #6ee7b7; background: rgba(16, 185, 129, 0.12); border-color: rgba(16, 185, 129, 0.4);
    }
    .mobile-loader.err {
      color: #fca5a5; background: rgba(239, 68, 68, 0.12); border-color: rgba(239, 68, 68, 0.4);
    }

    /* Colonne droite : Fiche d'identité */
    .empty-state {
      text-align: center; color: #93a4c3; padding: 65px 20px;
      border: 1px dashed #243252; border-radius: 12px;
    }
    .frida-action-bar {
      display: flex; gap: 10px; flex-wrap: wrap; margin-bottom: 16px;
      padding: 12px; border-radius: 10px; background: rgba(16, 185, 129, 0.1);
      border: 1px solid rgba(16, 185, 129, 0.35); align-items: center;
    }
    .btn-confirm-frida {
      flex: 1; padding: 11px 16px; border: 0; border-radius: 9px;
      background: linear-gradient(180deg, #10b981, #059669); color: #042f2e;
      font-size: 14px; font-weight: 700; cursor: pointer;
    }
    .btn-confirm-frida:hover { filter: brightness(1.06); }
    .id-header {
      display: flex; gap: 18px; align-items: flex-start;
      padding-bottom: 16px; border-bottom: 1px solid #243252; flex-wrap: wrap;
    }
    .photo-col { display: flex; flex-direction: column; align-items: center; gap: 4px; flex-shrink: 0; }
    .photo {
      width: 118px; height: 150px; border-radius: 10px; background: #17233d;
      border: 1px solid #10b981; object-fit: cover;
      display: flex; align-items: center; justify-content: center;
      color: #93a4c3; font-size: 11px; text-align: center;
    }
    .photo-caption { font-size: 10.5px; color: #93a4c3; font-weight: 600; text-transform: uppercase; }
    .names { flex: 1; min-width: 220px; }
    .name-lat { font-size: 21px; font-weight: 700; margin: 0; color: #ffffff; }
    .name-ar {
      font-size: 24px; font-weight: 600; direction: rtl; text-align: right;
      margin: 4px 0 8px 0; color: #6ee7b7;
      font-family: "Traditional Arabic", "Amiri", "Segoe UI", sans-serif;
    }
    .nin-badge {
      display: inline-block; font-family: Consolas, monospace; font-size: 13.5px; font-weight: 600;
      background: rgba(16, 185, 129, 0.14); color: #6ee7b7; border: 1px solid rgba(16, 185, 129, 0.35);
      padding: 4px 10px; border-radius: 6px; letter-spacing: 0.5px;
    }
    .badges { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; }
    .badge {
      font-size: 11px; font-weight: 600; padding: 4px 9px; border-radius: 999px;
      border: 1px solid #243252; background: #17233d; color: #cbd5e1;
    }
    .badge.VALID { background: rgba(16, 185, 129, 0.16); border-color: #10b981; color: #6ee7b7; }
    .badge.INVALID { background: rgba(239, 68, 68, 0.16); border-color: #ef4444; color: #fca5a5; }
    .badge.NOT_CHECKED { background: rgba(245, 158, 11, 0.16); border-color: #f59e0b; color: #fcd34d; }
    .summary-line {
      margin-top: 7px; font-size: 12px; color: #93a4c3;
      padding: 6px 10px; border-radius: 6px; background: #17233d;
    }
    .summary-line.bad {
      background: rgba(239, 68, 68, 0.14); color: #fca5a5; border: 1px solid rgba(239, 68, 68, 0.4);
    }
    .fields-grid {
      display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px 16px; margin-top: 16px;
    }
    @media (max-width: 600px) { .fields-grid { grid-template-columns: 1fr; } }
    .field-item {
      padding: 8px 11px; background: #17233d; border-radius: 8px; border: 1px solid #243252;
    }
    .field-item .k { font-size: 10.5px; color: #93a4c3; text-transform: uppercase; letter-spacing: .04em; }
    .field-item .v { font-size: 13.5px; margin-top: 3px; font-weight: 500; color: #e6edf7; }
    .field-item .ar {
      direction: rtl; text-align: right; color: #6ee7b7; font-size: 15px; display: block; margin-top: 2px;
    }
    .warnings-box {
      margin-top: 14px; padding: 9px 12px; background: rgba(245, 158, 11, 0.12);
      border: 1px solid rgba(245, 158, 11, 0.4); border-radius: 8px; font-size: 12px; color: #fcd34d;
    }
    .json-pre {
      background: #070c17; border: 1px solid #243252; border-radius: 8px;
      padding: 10px; font-size: 11px; overflow: auto; max-height: 240px; color: #cbd5e1;
    }
    .spinner {
      display: inline-block; width: 14px; height: 14px;
      border: 2px solid rgba(255,255,255,0.3); border-top-color: currentColor;
      border-radius: 50%; animation: spin 0.9s linear infinite;
      vertical-align: middle; margin-right: 6px;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class NfcScannerModalComponent implements OnInit, OnDestroy {
  @Input() mode: 'mobile' | 'usb' = 'mobile';
  @Output() closeModal = new EventEmitter<void>();
  @Output() nfcDataReceived = new EventEmitter<NfcData>();

  // État commun
  successData: NfcData | null = null;
  private dataAlreadyEmitted = false;
  jsonCopied = false;

  // ==================== État Mode USB (dz-eid-agent) ====================
  agentDotState: 'ok' | 'ko' | 'wait' = 'wait';
  agentStatusText = 'Agent…';
  readerDotState: 'ok' | 'ko' | 'wait' = 'wait';
  readerStatusText = 'Lecteur…';
  readersTooltip = '';
  cardDotState: 'ok' | 'ko' | 'wait' | '' = '';
  cardStatusText = 'Carte…';

  mrzRaw = '';
  mrzDoc = '';
  mrzDob = '';
  mrzExp = '';
  readPhotoOpt = false;
  readSignatureOpt = false;

  isReadingUsb = false;
  usbError = '';
  showProgress = false;
  progressPct = 0;
  progressText = '';
  logs: string[] = [];

  // Douchette MRZ en mode USB (/v1/relay sur port 8989)
  showUsbQrBox = false;
  usbRelaySvgUrl: SafeUrl | null = null;
  usbFallbackQrData = '';
  usbRelayAddresses: string[] = [];
  usbRelaySelectedHost = '';
  usbRelayStatusText = '⏳ Ouverture de la session…';
  usbRelayError = false;

  // ==================== État Mode Mobile E2EE (dz-eid-relay) ====================
  qrData = '';
  mobileRelayUrl = '';
  mobileAddresses: string[] = [];
  selectedMobileHost = '';
  adresseNonConfiguree = false;
  mobileRelayDotState: 'ok' | 'ko' | 'wait' = 'wait';
  mobileRelayStatusText = 'Connexion au relais…';
  mobileLoaderText = '⏳ Initialisation de la session chiffrée…';
  mobileError = '';

  private usbWsSub?: Subscription;
  private usbRelaySub?: Subscription;
  private fridaFallbackSub?: Subscription;
  private mobileSessionInfo?: MobileE2eeSessionInfo;

  constructor(
    private parametresService: ParametresService,
    private authService: AuthService,
    private nfcService: NfcService,
    private sanitizer: DomSanitizer,
    private el: ElementRef,
    private renderer: Renderer2,
    private cdr: ChangeDetectorRef
  ) {}

  get clesSaisiesValides(): boolean {
    const dateRe = /^\d{6}$/;
    return (
      !!this.mrzDoc?.trim() &&
      dateRe.test(this.mrzDob?.trim() || '') &&
      dateRe.test(this.mrzExp?.trim() || '')
    );
  }

  get allWarnings(): string[] {
    if (!this.successData) return [];
    const w = [...(this.successData.warnings || [])];
    const paDetails = this.successData.verification?.passiveAuthentication?.details || [];
    return [...w, ...paDetails];
  }

  get truncatedJsonDisplay(): string {
    if (!this.successData?.rawJson) return '';
    try {
      const copy = JSON.parse(JSON.stringify(this.successData.rawJson));
      for (const k of ['photo', 'signatureImage']) {
        if (copy[k]?.base64) {
          const len = copy[k].base64.length;
          copy[k].base64 = `${copy[k].base64.substring(0, 60)}… (${len} caractères)`;
        }
      }
      return JSON.stringify(copy, null, 2);
    } catch {
      return '';
    }
  }

  ngOnInit() {
    this.renderer.appendChild(document.body, this.el.nativeElement);
    if (this.mode === 'usb') {
      this.initUsbMode();
    } else {
      this.initMobileMode();
    }
  }

  ngOnDestroy() {
    this.cleanupSubscriptions();
    if (this.el.nativeElement && this.el.nativeElement.parentNode === document.body) {
      this.renderer.removeChild(document.body, this.el.nativeElement);
    }
  }

  private cleanupSubscriptions() {
    if (this.usbWsSub) {
      this.usbWsSub.unsubscribe();
      this.usbWsSub = undefined;
    }
    if (this.usbRelaySub) {
      this.usbRelaySub.unsubscribe();
      this.usbRelaySub = undefined;
    }
    if (this.fridaFallbackSub) {
      this.fridaFallbackSub.unsubscribe();
      this.fridaFallbackSub = undefined;
    }
    if (this.mobileSessionInfo) {
      this.mobileSessionInfo.disconnect();
      this.mobileSessionInfo = undefined;
    }
  }

  // ==================== Actions de fermeture / validation ====================

  onOverlayClick() {
    this.close();
  }

  close() {
    // Si une identité a été lue et n'a pas encore été émise, on la transmet automatiquement à Frida
    if (this.successData && !this.dataAlreadyEmitted) {
      this.dataAlreadyEmitted = true;
      this.nfcDataReceived.emit(this.successData);
    }
    this.closeModal.emit();
  }

  confirmerEtAjouter() {
    if (!this.successData) return;
    this.dataAlreadyEmitted = true;
    this.nfcDataReceived.emit(this.successData);
    this.closeModal.emit();
  }

  reinitialiserLecture() {
    this.successData = null;
    this.dataAlreadyEmitted = false;
    this.usbError = '';
    this.showProgress = false;
    if (this.mode === 'mobile') {
      this.restartMobileSession();
    }
    this.cdr.markForCheck();
  }

  // ==================== MODE USB (dz-eid-agent Standard) ====================

  private initUsbMode() {
    this.refreshUsbStatus();
    this.connectUsbWebSocket();
  }

  private addLog(msg: string) {
    const line = `${new Date().toLocaleTimeString('fr-FR')}  ${msg}`;
    this.logs = [line, ...this.logs.slice(0, 29)];
    this.cdr.markForCheck();
  }

  private showReaders(readers?: UsbReaderInfo[]) {
    if (!readers || readers.length === 0) {
      this.readerDotState = 'ko';
      this.readerStatusText = 'Aucun lecteur';
      this.cardDotState = '';
      this.cardStatusText = 'Carte —';
      this.readersTooltip = '';
      return;
    }
    const shortName = (n: string) => n.replace(/\s+\d+$/, '');
    const withCard = readers.find(r => r.cardPresent);
    this.readerStatusText = withCard
      ? shortName(withCard.name)
      : readers.length === 1
      ? shortName(readers[0].name)
      : `${readers.length} lecteurs`;
    this.readersTooltip = readers.map(r => r.name).join('\n');
    this.readerDotState = 'ok';
    this.cardDotState = withCard ? 'ok' : 'wait';
    this.cardStatusText = withCard ? 'Carte posée' : 'Pas de carte';
  }

  private refreshUsbStatus() {
    this.nfcService.getUsbAgentStatus().subscribe(status => {
      if (status) {
        this.agentDotState = 'ok';
        this.agentStatusText = `Agent ${status.version || ''}`.trim();
        this.showReaders(status.readers);
      } else {
        this.agentDotState = 'ko';
        this.agentStatusText = 'Agent injoignable';
        this.readerDotState = 'ko';
        this.readerStatusText = 'Aucun lecteur';
        this.cardDotState = '';
        this.cardStatusText = 'Carte —';
      }
      this.cdr.markForCheck();
    });
  }

  private connectUsbWebSocket() {
    if (this.usbWsSub) this.usbWsSub.unsubscribe();
    this.usbWsSub = this.nfcService.connectUsbEvents().subscribe({
      next: (ev: any) => {
        switch (ev.type) {
          case 'WS_OPEN':
            this.addLog("Connecté aux événements de l'agent");
            break;
          case 'HELLO':
          case 'READERS_CHANGED':
            this.agentDotState = 'ok';
            if (ev.version) this.agentStatusText = `Agent ${ev.version}`;
            this.showReaders(ev.readers);
            if (ev.type !== 'HELLO') {
              this.addLog(`Lecteurs : ${(ev.readers || []).length}`);
            }
            break;
          case 'CARD_INSERTED':
            this.refreshUsbStatus();
            this.addLog(`Carte posée (${ev.reader || 'lecteur'})`);
            break;
          case 'CARD_REMOVED':
            this.refreshUsbStatus();
            this.addLog(`Carte retirée (${ev.reader || 'lecteur'})`);
            break;
          case 'WAITING_FOR_CARD':
            this.setProgress(10, 'Posez la carte sur le lecteur…');
            this.addLog('En attente de la carte');
            break;
          case 'PROGRESS':
            this.setProgress(ev.percent, ev.message);
            break;
          case 'READ_COMPLETED':
            this.addLog(`Lecture terminée en ${ev.durationMs} ms`);
            break;
          case 'READ_FAILED':
            this.addLog(`Échec : ${ev.code} — ${ev.message || ''}`);
            break;
          case 'WS_CLOSE':
            this.agentDotState = 'ko';
            this.agentStatusText = 'Agent déconnecté';
            break;
        }
        this.cdr.markForCheck();
      }
    });
  }

  private setProgress(pct: number, text: string) {
    this.showProgress = true;
    this.progressPct = pct || 0;
    this.progressText = text || '';
    this.cdr.markForCheck();
  }

  onMrzRawInput() {
    const parsed = this.nfcService.parseMrzString(this.mrzRaw);
    if (parsed) {
      this.mrzDoc = parsed.doc;
      this.mrzDob = parsed.dob;
      this.mrzExp = parsed.doe;
      this.usbError = '';
      this.cdr.markForCheck();
    }
  }

  toggleUsbMrzRelay() {
    if (this.showUsbQrBox) {
      this.showUsbQrBox = false;
      if (this.usbRelaySub) {
        this.usbRelaySub.unsubscribe();
        this.usbRelaySub = undefined;
      }
      return;
    }
    this.openUsbMrzRelay(null);
  }

  onUsbRelayHostChange() {
    if (this.usbRelaySelectedHost) {
      this.openUsbMrzRelay(this.usbRelaySelectedHost);
    }
  }

  private openUsbMrzRelay(host: string | null) {
    if (this.usbRelaySub) {
      this.usbRelaySub.unsubscribe();
      this.usbRelaySub = undefined;
    }
    this.showUsbQrBox = true;
    this.usbRelayError = false;
    this.usbRelayStatusText = '⏳ Ouverture de la session…';
    this.cdr.markForCheck();

    // Utilise nativement POST http://127.0.0.1:8989/v1/relay de dz-eid-agent (port 8989 ouvert au pare-feu)
    this.nfcService.openUsbMrzRelay(host).subscribe({
      next: (data) => {
        this.usbFallbackQrData = '';
        this.usbRelaySvgUrl = this.sanitizer.bypassSecurityTrustUrl(
          'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(data.qrSvg)
        );
        this.usbRelayAddresses = Array.isArray(data.addresses) ? data.addresses : [];
        this.usbRelaySelectedHost = data.host;
        const mins = Math.max(1, Math.round((data.expiresInSeconds || 300) / 60));
        this.usbRelayStatusText = `⏳ En attente du scan MRZ… (expire dans ${mins} min)`;
        this.cdr.markForCheck();

        this.usbRelaySub = this.nfcService.listenUsbMrzRelay(data.sessionId).subscribe({
          next: (ev) => {
            if (ev.type === 'MRZ_DATA') {
              this.applyReceivedMrzAndRead(ev.payload);
            }
          },
          error: (err) => {
            this.usbRelayError = true;
            this.usbRelayStatusText = `❌ ${err?.message || 'Session expirée ou interrompue'}`;
            this.cdr.markForCheck();
          }
        });
      },
      error: () => {
        // Repli sur le relais MRZ du backend Frida si dz-eid-agent n'a pas le relais activé
        this.openFallbackFridaMrzRelay();
      }
    });
  }

  private openFallbackFridaMrzRelay() {
    forkJoin({
      parametres: this.parametresService.lire().pipe(catchError(() => of({} as any))),
      ipsAgent: this.nfcService.getLanAddresses()
    }).subscribe(({ parametres, ipsAgent }) => {
      const hote = window.location.hostname;
      const estLocal = hote === 'localhost' || hote === '127.0.0.1';
      const adresse = !estLocal ? hote : (ipsAgent[0] || (parametres?.adresseReseauLocale || '').trim());
      if (!adresse) {
        this.usbRelayError = true;
        this.usbRelayStatusText = '❌ Agent local injoignable et aucune adresse réseau configurée.';
        this.cdr.markForCheck();
        return;
      }
      const sessionId = uuidv4();
      const port = window.location.port ? ':' + window.location.port : '';
      const targetUrl = `${window.location.protocol}//${adresse}${port}/api/nfc-session/${sessionId}/mrz-only`;
      this.usbRelaySvgUrl = null;
      this.usbFallbackQrData = JSON.stringify({ action: 'nfc_upload', url: targetUrl });
      this.usbRelayStatusText = `⏳ En attente du scan MRZ sur ${adresse}…`;
      this.cdr.markForCheck();

      this.usbRelaySub = this.nfcService.listenToMobileNfc('/api', sessionId).subscribe({
        next: (ev) => {
          if (ev.type === 'MRZ_DATA') {
            this.applyReceivedMrzAndRead(ev.payload);
          }
        }
      });
    });
  }

  private applyReceivedMrzAndRead(mrz: any) {
    if (!mrz) return;
    if (mrz.raw) {
      this.mrzRaw = mrz.raw;
      this.onMrzRawInput();
    }
    if (mrz.documentNumber) {
      this.mrzDoc = String(mrz.documentNumber).replace(/</g, '').trim();
    }
    if (mrz.dateOfBirth) {
      this.mrzDob = this.formatToYymmdd(mrz.dateOfBirth);
    }
    if (mrz.expiryDate) {
      this.mrzExp = this.formatToYymmdd(mrz.expiryDate);
    }
    this.showUsbQrBox = false;
    this.addLog('MRZ reçue depuis la douchette mobile');
    this.cdr.detectChanges();
    if (this.clesSaisiesValides) {
      this.lireUsb();
    }
  }

  private formatToYymmdd(d: any): string {
    if (typeof d === 'string') {
      const clean = d.trim();
      if (/^\d{6}$/.test(clean)) return clean;
      const parts = clean.split('-');
      if (parts.length === 3) {
        return parts[0].substring(2) + parts[1] + parts[2];
      }
      return this.nfcService.normalizeMrzDigits(clean);
    }
    if (Array.isArray(d) && d.length >= 3) {
      const y = String(d[0]).substring(2);
      const m = String(d[1]).padStart(2, '0');
      const day = String(d[2]).padStart(2, '0');
      return y + m + day;
    }
    return '';
  }

  lireUsb() {
    this.usbError = '';
    if (!this.clesSaisiesValides) {
      this.usbError = 'Veuillez renseigner le N° de document et les dates (AAMMJJ).';
      return;
    }

    this.mrzDoc = this.mrzDoc.trim().toUpperCase();
    this.mrzDob = this.mrzDob.trim();
    this.mrzExp = this.mrzExp.trim();

    this.isReadingUsb = true;
    this.setProgress(3, 'Démarrage de la lecture…');

    this.nfcService
      .readViaUsb(
        this.mrzDoc,
        this.mrzDob,
        this.mrzExp,
        this.mrzRaw,
        this.readPhotoOpt,
        this.readSignatureOpt,
        false
      )
      .subscribe({
        next: (data) => {
          this.isReadingUsb = false;
          this.setProgress(100, 'Lecture terminée.');
          this.handleSuccess(data);
        },
        error: (err) => {
          this.isReadingUsb = false;
          this.showProgress = false;
          const apiErr = err?.error?.error || err?.error;
          const code = apiErr?.code ? `[${apiErr.code}] ` : '';
          const msg = apiErr?.message || err?.message || 'Erreur de lecture de la puce. Vérifiez la MRZ et la position de la carte.';
          this.usbError = `${code}${msg}`;
          this.cdr.markForCheck();
        }
      });
  }

  // ==================== MODE MOBILE NFC (dz-eid E2EE Standard) ====================

  private initMobileMode() {
    this.startMobileE2eeSession();
  }

  restartMobileSession() {
    if (this.mobileSessionInfo) {
      this.mobileSessionInfo.disconnect();
      this.mobileSessionInfo = undefined;
    }
    if (this.fridaFallbackSub) {
      this.fridaFallbackSub.unsubscribe();
      this.fridaFallbackSub = undefined;
    }
    this.qrData = '';
    this.mobileError = '';
    this.startMobileE2eeSession();
  }

  private startMobileE2eeSession() {
    this.mobileRelayDotState = 'wait';
    this.mobileRelayStatusText = 'Ouverture de session…';
    this.mobileLoaderText = '⏳ Génération de la clé ECDH P-256 et du QR Code…';
    this.adresseNonConfiguree = false;
    this.cdr.markForCheck();

    forkJoin({
      parametres: this.parametresService.lire().pipe(catchError(() => of({} as any))),
      ipsAgent: this.nfcService.getLanAddresses()
    }).subscribe(async ({ parametres, ipsAgent }) => {
      const paramIp = (parametres?.adresseReseauLocale || '').trim();
      // Si l'agent ou le relais détecte une IP Wi-Fi/LAN en direct différente de celle en base, mettre à jour la base automatiquement
      if (ipsAgent.length > 0 && ipsAgent[0] && ipsAgent[0] !== paramIp) {
        this.parametresService.modifier({ adresseReseauLocale: ipsAgent[0] }).pipe(catchError(() => of(null))).subscribe();
      }
      const fallbackIps = Array.from(new Set([...ipsAgent, ...(paramIp ? [paramIp] : [])])).filter(Boolean);

      try {
        const session = await this.nfcService.initMobileE2eeSession(
          fallbackIps,
          (nfcData) => {
            this.mobileLoaderText = '✅ Identité reçue et déchiffrée avec succès !';
            this.handleSuccess(nfcData);
          },
          (err) => {
            console.error('Erreur session Mobile NFC :', err);
          }
        );

        this.mobileSessionInfo = session;
        this.mobileAddresses = session.addresses;
        this.selectedMobileHost = session.host;
        this.mobileRelayUrl = session.mobileRelayUrl;
        this.qrData = session.qrPayload;

        if (!session.host) {
          this.adresseNonConfiguree = true;
          this.mobileRelayDotState = 'ko';
          this.mobileRelayStatusText = 'Adresse LAN manquante';
        } else {
          this.mobileRelayDotState = 'ok';
          this.mobileRelayStatusText = session.mode === 'dzeid-relay' ? 'Relais E2EE prêt' : 'Relais E2EE Frida prêt';
          this.mobileLoaderText = '📱 En attente du scan QR et de la lecture NFC sur le téléphone…';
        }
        this.cdr.markForCheck();
      } catch (e: any) {
        this.mobileError = e?.message || 'Erreur initialisation relais mobile';
        this.mobileRelayDotState = 'ko';
        this.mobileRelayStatusText = 'Relais injoignable';
        this.mobileLoaderText = '❌ Erreur de connexion au relais E2EE.';
        this.cdr.markForCheck();
      }
    });
  }

  onMobileHostChange() {
    const cleanHost = (this.selectedMobileHost || '')
      .trim()
      .replace(/^https?:\/\//i, '')
      .replace(/\/.*$/, '')
      .replace(/:\d+$/, '');
    if (!cleanHost) return;
    this.selectedMobileHost = cleanHost;
    this.adresseNonConfiguree = false;
    this.parametresService.modifier({ adresseReseauLocale: cleanHost }).pipe(catchError(() => of(null))).subscribe();
    if (this.mobileSessionInfo) {
      this.qrData = this.mobileSessionInfo.updateHost(cleanHost);
      this.mobileRelayUrl = this.mobileSessionInfo.mobileRelayUrl;
      this.mobileRelayDotState = 'ok';
      this.mobileRelayStatusText = 'Relais E2EE prêt';
      this.cdr.markForCheck();
    }
  }

  // ==================== Traitement et formatage du résultat ====================

  private handleSuccess(data: NfcData) {
    this.successData = this.nfcService.mapIdentityRecordToNfcData(data?.rawJson || data, data?.documentNumber);
    this.cdr.detectChanges();
  }

  frStatus(s?: string): string {
    switch (s) {
      case 'VALID':
        return 'Valide';
      case 'INVALID':
        return 'INVALIDE';
      case 'NOT_CHECKED':
        return 'Non vérifié';
      case 'NOT_SUPPORTED':
        return 'Non supporté';
      default:
        return s || '—';
    }
  }

  fmtDate(iso?: string): string {
    if (!iso) return '—';
    if (/^\d{4}-\d{2}-\d{2}$/.test(iso)) {
      return iso.split('-').reverse().join('/');
    }
    return iso;
  }

  isExpired(iso?: string): boolean {
    if (!iso || !/^\d{4}-\d{2}-\d{2}$/.test(iso)) return false;
    return new Date(iso) < new Date();
  }

  copyJson() {
    if (!this.successData?.rawJson) return;
    navigator.clipboard.writeText(JSON.stringify(this.successData.rawJson, null, 2)).then(() => {
      this.jsonCopied = true;
      this.cdr.markForCheck();
      setTimeout(() => {
        this.jsonCopied = false;
        this.cdr.markForCheck();
      }, 2000);
    });
  }
}
