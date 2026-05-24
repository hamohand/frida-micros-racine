import { Injectable } from '@angular/core';
import { UploadWindowState } from '../components/dossier/upload-windows/upload-window.interface';

@Injectable({
  providedIn: 'root'
})
export class UploadStateService {
  private windowsState: Record<string, UploadWindowState> | null = null;
  private ocrMode: 'rapide' | 'approfondi' | 'batch' | null = null;

  saveState(windows: Record<string, UploadWindowState>, mode: 'rapide' | 'approfondi' | 'batch') {
    this.windowsState = windows;
    this.ocrMode = mode;
  }

  getState() {
    return { windows: this.windowsState, ocrMode: this.ocrMode };
  }

  clearState() {
    this.windowsState = null;
    this.ocrMode = null;
  }
}
