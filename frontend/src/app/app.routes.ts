import { Routes } from '@angular/router';
import { HomeComponent } from './components/accueil/home/home.component';
import { AboutComponent } from './components/accueil/about/about.component';
import { CreatePersonComponent } from './components/dossier/create-person/create-person.component';
import { UploadWindowsComponent } from './components/dossier/upload-windows/upload-windows.component';
import {LireaiEcrirebdComponent} from "./components/aibd/lireaiEcrirebd";
import {FridaComponent} from "./components/frida/frida.component";
import {AdminComponent} from "./components/admin/admin.component";
import {SearchComponent} from "./components/search/search.component";
import {FridaListComponent} from "./components/frida-list/frida-list.component";

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'about', component: AboutComponent },
  { path: 'create', component: CreatePersonComponent },
  { path: 'upload', component: UploadWindowsComponent },
  { path: 'frida', component: FridaComponent},
  {path: 'search', component: SearchComponent},
  { path: 'list', component: FridaListComponent },
  { path: '**', redirectTo: '' }
];