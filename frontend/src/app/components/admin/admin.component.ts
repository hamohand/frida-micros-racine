import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from "@angular/router";

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [RouterOutlet, RouterLink],
  template: `
    <nav class="nav-bar">
      <a routerLink="/" class="nav-logo">Ustadh-a</a>
      <div class="nav-links">
        <a routerLink="/" class="nav-link">
          <svg xmlns="http://www.w3.org/2000/svg" height="24px" viewBox="0 -960 960 960" width="24px" fill="#e8eaed"><path d="M240-200h120v-240h240v240h120v-360L480-740 240-560v360Zm-80 80v-480l320-240 320 240v480H520v-240h-80v240H160Zm320-350Z"/></svg>
        </a>
        <a routerLink="/create" class="nav-link">Créer</a>
        <a routerLink="/search" class="nav-link">Rechercher</a>
        <a routerLink="/list" class="nav-link">Liste</a>
<!--        <a routerLink="/about" class="nav-link">À propos</a>-->
      </div>
    </nav>
    <main>
      <router-outlet></router-outlet>
    </main>
  `,
})
export class AdminComponent {

}
