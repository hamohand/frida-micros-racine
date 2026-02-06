import { Component } from '@angular/core';

@Component({
  selector: 'app-about',
  standalone: true,
  template: `
    <div class="container">
      <h1 class="hero-title">À propos</h1>
      <div class="about-content">
        <p>
          Notre application est conçue pour vous offrir une expérience utilisateur
          exceptionnelle avec des fonctionnalités puissantes et une interface élégante.
        </p>
        <a href="#" class="btn btn-primary">Commencer</a>
      </div>
    </div>
  `,
  styles: [`
    .about-content {
      max-width: 800px;
      margin: var(--spacing-lg) auto;
      color: var(--text-secondary);
    }
  `]
})
export class AboutComponent {}