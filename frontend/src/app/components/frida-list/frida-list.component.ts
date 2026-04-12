import { Component, OnInit } from '@angular/core';
import { FridaService } from '../../services/frida.service';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-frida-list',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="list-wrapper">
      <div class="glass-panel">
        <div class="header-section">
          <h2>Archive des Fridas</h2>
          <div class="search-box">
            <svg xmlns="http://www.w3.org/2000/svg" height="24" viewBox="0 -960 960 960" width="24" fill="currentColor"><path d="M784-120 532-372q-30 24-69 38t-83 14q-109 0-184.5-75.5T120-580q0-109 75.5-184.5T380-840q109 0 184.5 75.5T640-580q0 44-14 83t-38 69l252 252-56 56ZM380-400q75 0 127.5-52.5T560-580q0-75-52.5-127.5T380-760q-75 0-127.5 52.5T200-580q0 75 52.5 127.5T380-400Z"/></svg>
            <input type="text" placeholder="Rechercher par nom, numéro ou date..." (input)="onSearch($event)" />
          </div>
        </div>

        <div class="table-container">
          <table class="table">
            <thead>
              <tr>
                <th>Numéro de Dossier</th>
                <th>Défunt (Nom Prénom)</th>
                <th>Date de Création</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let frida of filteredList">
                <td class="font-mono">
                  <strong>{{ frida?.numFrida }}</strong>
                  <span *ngIf="frida?.requiresCorrection" class="badge-warning" style="margin-left: 8px;">⚠️ À corriger</span>
                </td>
                <td>{{ frida?.nom }} {{ frida?.prenom }}</td>
                <td><span class="badge">{{ frida?.dateCreation }}</span></td>
                <td>
                  <button class="btn-action" 
                          (click)="voirFrida(frida.numFrida, frida.requiresCorrection)" 
                          [title]="frida.requiresCorrection ? 'Corriger les données' : 'Consulter l\\'acte'">
                    <svg *ngIf="!frida.requiresCorrection" xmlns="http://www.w3.org/2000/svg" height="24" viewBox="0 -960 960 960" width="24" fill="currentColor"><path d="M200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h280v80H200v560h560v-280h80v280q0 33-23.5 56.5T760-120H200Zm188-212-56-56 372-372H560v-80h280v280h-80v-144L388-332Z"/></svg>
                    <svg *ngIf="frida.requiresCorrection" xmlns="http://www.w3.org/2000/svg" height="24" viewBox="0 -960 960 960" width="24" fill="currentColor"><path d="M200-200h57l391-391-57-57-391 391v57Zm-80 80v-170l528-527q12-11 26.5-17t30.5-6q16 0 31 6t26 18l55 56q11 11 17 25.5t6 30.5q0 16-6 30.5t-17 25.5L290-120H120Zm640-584-56-56 56 56Zm-141 85-28-29 57 57-29-28Z"/></svg>
                    <span>{{ frida.requiresCorrection ? 'Corriger' : 'Ouvrir' }}</span>
                  </button>
                </td>
              </tr>
              <tr *ngIf="filteredList.length === 0">
                <td colspan="4" class="empty-state">
                  Aucun document trouvé pour cette recherche.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .list-wrapper {
      min-height: calc(100vh - 80px);
      padding: 3rem 2rem;
      background: #0f1c15;
      display: flex;
      justify-content: center;
      align-items: flex-start;
      animation: fadeIn 0.8s ease-out;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(10px); }
      to { opacity: 1; transform: translateY(0); }
    }

    .glass-panel {
      background: rgba(255, 255, 255, 0.03);
      backdrop-filter: blur(16px);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 16px;
      padding: 2rem;
      width: 100%;
      max-width: 1100px;
      box-shadow: 0 15px 35px rgba(0,0,0,0.4);
    }

    .header-section {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 2rem;
      flex-wrap: wrap;
      gap: 1rem;
    }

    h2 {
      color: #ffffff;
      margin: 0;
      font-size: 2rem;
    }

    .search-box {
      display: flex;
      align-items: center;
      background: rgba(0, 0, 0, 0.2);
      border: 1px solid rgba(255, 255, 255, 0.2);
      border-radius: 8px;
      padding: 0.5rem 1rem;
      width: 100%;
      max-width: 400px;
      transition: all 0.3s ease;
    }
    
    .search-box:focus-within {
      border-color: #48bb78;
      box-shadow: 0 0 0 2px rgba(72, 187, 120, 0.2);
    }

    .search-box svg {
      color: #a0aec0;
      margin-right: 0.5rem;
    }

    .search-box input {
      background: transparent;
      border: none;
      color: #fff;
      font-size: 1rem;
      width: 100%;
      outline: none;
    }

    .table-container {
      overflow-x: auto;
    }

    .table {
      width: 100%;
      border-collapse: separate;
      border-spacing: 0;
      text-align: left;
    }

    .table th {
      padding: 1rem;
      color: #48bb78;
      background: rgba(0, 0, 0, 0.2);
      font-weight: 600;
      text-transform: uppercase;
      font-size: 0.9rem;
      letter-spacing: 0.05em;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    }

    .table td {
      padding: 1rem;
      color: #e2e8f0;
      border-bottom: 1px solid rgba(255, 255, 255, 0.05);
      vertical-align: middle;
    }

    .table tbody tr {
      transition: background-color 0.2s ease;
    }

    .table tbody tr:hover {
      background: rgba(255, 255, 255, 0.05);
    }

    .font-mono {
      font-family: monospace;
      letter-spacing: 1px;
      color: #63b3ed;
    }

    .badge {
      background: rgba(72, 187, 120, 0.15);
      color: #48bb78;
      padding: 0.25rem 0.75rem;
      border-radius: 9999px;
      font-size: 0.85rem;
      font-weight: 500;
    }

    .badge-warning {
      background: rgba(237, 137, 54, 0.15);
      color: #ed8936;
      border: 1px solid rgba(237, 137, 54, 0.3);
      padding: 0.2rem 0.5rem;
      border-radius: 6px;
      font-size: 0.75rem;
      font-weight: bold;
    }

    .btn-action {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      background: rgba(66, 153, 225, 0.15);
      color: #63b3ed;
      border: 1px solid rgba(66, 153, 225, 0.3);
      padding: 0.5rem 1rem;
      border-radius: 6px;
      cursor: pointer;
      font-weight: 500;
      transition: all 0.2s;
    }

    .btn-action:hover {
      background: rgba(66, 153, 225, 0.3);
      color: #fff;
    }

    .empty-state {
      text-align: center;
      padding: 3rem !important;
      color: #a0aec0 !important;
      font-style: italic;
    }
  `]
})
export class FridaListComponent implements OnInit {
  fridaList: { numFrida: string; dateCreation: string; dateNaissane: string; nom: string; prenom?: string; requiresCorrection?: boolean }[] = [];
  filteredList: { numFrida: string; dateCreation: string; dateNaissane: string; nom: string; prenom?: string; requiresCorrection?: boolean }[] = [];
  url: string = 'http://localhost:8080/api/frida/';

  constructor(private fridaService: FridaService, private router: Router) { }

  ngOnInit(): void {
    this.fridaService.lancerApi(this.url + "fridas").subscribe({
      next: data => {
        if (data && Array.isArray(data)) {
          this.fridaList = data.filter(f => f != null);
          // Trier par date du plus récent au plus ancien (si besoin)
          this.fridaList.sort((a, b) => b.numFrida.localeCompare(a.numFrida));
          this.filteredList = [...this.fridaList];
        }
      }
    });
  }

  onSearch(event: any) {
    const term = event.target.value.toLowerCase();

    if (!term) {
      this.filteredList = [...this.fridaList];
    } else {
      this.filteredList = this.fridaList.filter(frida =>
        (frida.nom && frida.nom.toLowerCase().includes(term)) ||
        (frida.numFrida && frida.numFrida.toLowerCase().includes(term)) ||
        (frida.dateCreation && frida.dateCreation.includes(term))
      );
    }
  }

  voirFrida(num: string, requiresCorrection?: boolean) {
    if (requiresCorrection) {
       this.router.navigate(['/edit', num]);
    } else {
       this.router.navigate(['/frida'], { queryParams: { numFrida: num } });
    }
  }
}
