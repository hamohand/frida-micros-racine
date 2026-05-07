import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Component({
  selector: 'app-frida-edit',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './frida-edit.component.html',
  styleUrl: './frida-edit.component.css'
})
export class FridaEditComponent implements OnInit {
  numFrida: string = '';
  frida: any = null;
  loading: boolean = true;
  saving: boolean = false;
  errorMsg: string = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient
  ) {}

  async ngOnInit() {
    this.numFrida = this.route.snapshot.paramMap.get('numFrida') || '';
    if (!this.numFrida) {
      this.errorMsg = 'Numéro de dossier manquant.';
      this.loading = false;
      return;
    }
    
    try {
      this.frida = await firstValueFrom(this.http.get(`/api/frida/${this.numFrida}`));
      this.loading = false;
    } catch (err) {
      console.error(err);
      this.errorMsg = 'Impossible de charger le dossier.';
      this.loading = false;
    }
  }

  async sauvegarderCorrections() {
    this.saving = true;
    try {
      await firstValueFrom(this.http.put(`/api/frida/corrections/${this.numFrida}`, this.frida));
      this.saving = false;
      // Rediriger vers l'archive avec succès
      this.router.navigate(['/list']);
    } catch (err) {
      console.error(err);
      this.errorMsg = 'Erreur lors de la sauvegarde. Veuillez vérifier les données.';
      this.saving = false;
    }
  }

  annuler() {
    this.router.navigate(['/list']);
  }
}
