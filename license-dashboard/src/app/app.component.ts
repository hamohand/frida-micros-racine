import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from './auth.service';
import { LicenseService, FridaLicense } from './license.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent implements OnInit {
  title = 'license-dashboard';
  licenses: FridaLicense[] = [];
  
  newNotaryName = '';
  newValidMonths = 12;

  constructor(
    public auth: AuthService,
    private licenseService: LicenseService
  ) {}

  ngOnInit() {
    // Only fetch licenses if user is authenticated
    setTimeout(() => {
      if (this.auth.claims) {
        this.loadLicenses();
      }
    }, 1500); // Wait for auth to init
  }

  loadLicenses() {
    this.licenseService.getLicenses().subscribe({
      next: (data) => this.licenses = data,
      error: (err) => console.error('Erreur chargement licences', err)
    });
  }

  generateLicense() {
    if (!this.newNotaryName) return;
    this.licenseService.createLicense(this.newNotaryName, this.newValidMonths).subscribe({
      next: () => {
        this.newNotaryName = '';
        this.newValidMonths = 12;
        this.loadLicenses();
      },
      error: (err) => alert('Erreur création')
    });
  }

  revokeLicense(id: string) {
    if (confirm('Voulez-vous vraiment révoquer cette licence ?')) {
      this.licenseService.revokeLicense(id).subscribe({
        next: () => this.loadLicenses(),
        error: (err) => alert('Erreur révocation')
      });
    }
  }

  logout() {
    this.auth.logout();
  }
}
