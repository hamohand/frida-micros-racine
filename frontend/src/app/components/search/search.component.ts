import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router } from "@angular/router";
import { FridaService } from "../../services/frida.service";

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
      <div class="search-container form-group" xmlns="http://www.w3.org/1999/html">
      <!--      Recherche par numéro de frida-->
      <div class="person-form">
        <h4>Recherche par numéro de frida</h4>
        <input
            type="text"
            placeholder="Entrez le numéro de Frida"
            [(ngModel)]="numFrida"
            class="search-input"
        /><br><br>
        <button (click)="onCherche()" class="btn btn-primary">Rechercher</button>
      </div>
      <br>
<!--      &lt;!&ndash;      Recherche par nom de défunt&ndash;&gt;
      <div class="person-form">
        <h4>Recherche par nom du défunt</h4>
        <input
            type="text"
            placeholder="Entrez le nom du défunt"
            [(ngModel)]="nomDefunt"
            class="search-input"
        /><br>
        <button (click)="nomAfficheDefunts()" class="btn btn-primary">Rechercher</button>
      </div>-->
      <div *ngIf="defunts.length > 0" class="search-results">
        <h3>Résultats :</h3>
        <select [(ngModel)]="numFrida" size="3">
            <option *ngFor="let defunt of defunts" value="{{ defunt.numFrida }}">{{ defunt.numFrida }}</option>
<!--            {{ defunt | json }}-->
        </select>
        <button (click)="onCherche()" class="btn btn-primary">Rechercher</button>
        
      </div>
      <div *ngIf="defunts.length === 0 && searchPerformed">
        <p>Aucun résultat trouvé"</p>
      </div>
    </div>
  `,
  styles: [
    `
      .search-container {
        padding-top: calc(var(--nav-height) + var(--spacing-xs));
        max-width: 600px;
      }
      .person-form {
        background: rgba(255, 255, 255, 0.05);
        padding: var(--spacing-md);
        border-radius: var(--border-radius);
        backdrop-filter: blur(10px);
        margin-left: 50px;
        margin-top: 50px;
      }
      .form-group {
        margin-bottom: var(--spacing-xs);
      }
      .search-input {
        width: 100%;
        padding: var(--spacing-xs);
        border: 1px solid rgba(255, 255, 255, 0.1);
        border-radius: var(--border-radius);
        background: rgba(255, 255, 255, 0.05);
        color: var(--text-primary);
        transition: var(--transition);
      }
      .search-button {
        padding: 10px 20px;
      }
      .search-results {
        margin-top: 20px;
        text-align: left;
      }
    `,
  ],
})
export class SearchComponent {
  numFrida: string = '';
  nomDefunt: string = '';
  defunts: any = [];
  url: string = 'http://localhost:8080/api/frida/';
  searchPerformed: boolean = false;


  constructor(private router: Router, private fridaService: FridaService) { }

  onSearch() {
    this.searchPerformed = true;
    console.log('Rechercher :', this.numFrida);

    // Simulation des résultats de recherche
    this.defunts =
      this.numFrida === '123'
        ? [{ id: 1, numFrida: this.numFrida, description: 'Exemple de résultat' }]
        : [];
  }

  // Lance 'numAfficheFrida()'
  onCherche() {
    this.searchPerformed = true;
    console.log('Rechercher :', this.numFrida);
    this.numAfficheFrida(this.numFrida);
  }

  // Affiche une frida avec numFrida
  numAfficheFrida(numFrida: string) {// ouvre une nouvelle fenêtre avec la frida
    this.searchPerformed = true;
    console.log('Processus onAfficheFrida !');
    //Avec passage de paramètre 'numFrida
    this.router.navigate(['/frida'], { queryParams: { numFrida: numFrida } });
    // Construire l'URL à partir du router
    /* const url = this.router.serializeUrl(
         this.router.createUrlTree(['/frida'], { queryParams: { numFrida: this.numFrida } }) // Chemin de destination
     );
     // Ouvrir dans un nouvel onglet
     window.open(url, '_blank');*/
  }

  // Affichage des défunts correspondants au nom de famille
  nomAfficheDefunts(): void {
    //héritiers de la frida  par le numéro de la frida ordonnés
    this.fridaService.lancerApi(this.url + 'listeDefunts/' + this.nomDefunt).subscribe({
      next: data => {
        if (data) {
          this.defunts = data;
          console.log("Défunts de même nom : ", this.defunts);
        } else {
          console.error('Données invalides reçues pour "defunts" :', data);
          this.defunts = []; // Éviter une référence invalide.
        }

      },
      error: err => console.log(err)
    });
  }
}
