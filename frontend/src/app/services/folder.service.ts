import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CreateFolderRequest {
  nom: string;
  prenom: string;
}

export interface FolderResponse {
  folderName: string;
  fullPath: string;
}

@Injectable({
  providedIn: 'root'
})
export class FolderService {
  private apiUrl = 'http://localhost:8080/api/folders';

  constructor(private http: HttpClient) {}

  createFolder(request: CreateFolderRequest): Observable<FolderResponse> {
    return this.http.post<FolderResponse>(`${this.apiUrl}/create`, request);
  }
}