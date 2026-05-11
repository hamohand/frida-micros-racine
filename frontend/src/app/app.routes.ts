
import { Routes } from '@angular/router';

export const routes: Routes = [
  { 
    path: '', 
    loadComponent: () => import('./components/accueil/home/home.component').then(m => m.HomeComponent) 
  },
  { 
    path: 'about', 
    loadComponent: () => import('./components/accueil/about/about.component').then(m => m.AboutComponent) 
  },
  { 
    path: 'create', 
    loadComponent: () => import('./components/dossier/create-person/create-person.component').then(m => m.CreatePersonComponent) 
  },
  { 
    path: 'upload', 
    loadComponent: () => import('./components/dossier/upload-windows/upload-windows.component').then(m => m.UploadWindowsComponent) 
  },
  { 
    path: 'frida', 
    loadComponent: () => import('./components/frida/frida.component').then(m => m.FridaComponent)
  },
  {
    path: 'search', 
    loadComponent: () => import('./components/search/search.component').then(m => m.SearchComponent)
  },
  { 
    path: 'list', 
    loadComponent: () => import('./components/frida-list/frida-list.component').then(m => m.FridaListComponent) 
  },
  { 
    path: 'edit/:numFrida', 
    loadComponent: () => import('./components/frida-edit/frida-edit.component').then(m => m.FridaEditComponent) 
  },
  { 
    path: 'backups', 
    loadComponent: () => import('./components/backup/backup.component').then(m => m.BackupComponent) 
  },
  { path: '**', redirectTo: '' }
];