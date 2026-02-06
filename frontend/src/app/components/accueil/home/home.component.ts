import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink],
  template: `
    <section class="hero-section">
      <h1 class="hero-title">Bienvenue sur Ustadh-a</h1>
      <p class="hero-subtitle">
        Une application de gestion notariale <br />Moderne et intuitive
        
      </p>
      <div class="button-group">
        <a routerLink="/about" class="btn btn-primary">En savoir plus</a>
        <a href="#" class="btn btn-secondary">Documentation</a>
      </div>
    </section>
  `,
  styles: [`
    .button-group {
      display: flex;
      gap: var(--spacing-md);
    }
  `]
})
export class HomeComponent { }