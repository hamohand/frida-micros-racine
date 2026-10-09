import { Component, OnInit } from '@angular/core';
import {AdminComponent} from "./components/admin/admin.component";
import {NgIf} from "@angular/common";
import {FridaComponent} from "./components/frida/frida.component";
import {LanguageService} from "./services/language.service";

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    AdminComponent,
    NgIf,
    FridaComponent
  ],
  template: '<app-admin></app-admin>'
})
export class AppComponent implements OnInit {
  constructor(private languageService: LanguageService) {}

  ngOnInit() {
    this.languageService.init();
  }
}
