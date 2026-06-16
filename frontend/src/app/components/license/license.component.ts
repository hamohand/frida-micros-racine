import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';

@Component({
  selector: 'app-license',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './license.component.html',
  styleUrls: ['./license.component.css']
})
export class LicenseComponent implements OnInit {
  licenseKey: string = '';
  hardwareId: string = '';
  errorMessage: string = '';
  successMessage: string = '';
  isLoading: boolean = true;
  isValid: boolean = false;

  constructor(private http: HttpClient, private router: Router) {}

  ngOnInit() {
    this.checkStatus();
  }

  checkStatus() {
    this.isLoading = true;
    this.http.get<any>('/api/license/status').subscribe({
      next: (res) => {
        this.isLoading = false;
        this.hardwareId = res.hardwareId;
        this.isValid = res.valid;
        if (this.isValid) {
          this.licenseKey = res.key;
        }
      },
      error: () => {
        this.isLoading = false;
        this.errorMessage = "Impossible de joindre le serveur local.";
      }
    });
  }

  activate() {
    if (!this.licenseKey) return;
    
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    this.http.post<any>('/api/license/activate', { licenseKey: this.licenseKey }).subscribe({
      next: (res) => {
        this.isLoading = false;
        if (res.success) {
          this.isValid = true;
          this.successMessage = "Licence activée avec succès ! Redirection...";
          setTimeout(() => {
            this.router.navigate(['/']);
          }, 2000);
        } else {
          this.errorMessage = res.message || "Erreur d'activation.";
        }
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage = "Erreur lors de l'activation de la licence.";
      }
    });
  }
}
