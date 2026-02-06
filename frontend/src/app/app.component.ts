import { Component } from '@angular/core';
import {AdminComponent} from "./components/admin/admin.component";
import {LireaiEcrirebdComponent} from "./components/aibd/lireaiEcrirebd";
import {NgIf} from "@angular/common";
import {FridaComponent} from "./components/frida/frida.component";

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    AdminComponent,
    NgIf,
    FridaComponent

  ],
  template: `
<!--    <app-admin *ngIf="!fridaCreee"></app-admin>-->
<!--    <app-frida *ngIf="fridaCreee"></app-frida>-->
<app-admin></app-admin>
  `

})
export class AppComponent {
  //fridaCreee = false;
}
