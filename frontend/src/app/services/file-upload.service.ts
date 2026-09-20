import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpEventType } from '@angular/common/http';
import { Observable, map } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class FileUploadService {
  private apiUrl = '/api/files';

  constructor(private http: HttpClient) {}

  uploadFiles(files: File[], path: string, folderName?: string): Observable<number> {
    const formData = new FormData();
    files.forEach(file => formData.append('files', file));
    formData.append('path', path);
    if (folderName) {
      formData.append('folderName', folderName);
    }

    return this.http.post(`${this.apiUrl}/upload`, formData, {
      reportProgress: true,
      observe: 'events'
    }).pipe(
      map(event => this.getUploadProgress(event))
    );
  }

  private getUploadProgress(event: HttpEvent<any>): number {
    switch (event.type) {
      case HttpEventType.UploadProgress:
        return Math.round((100 * event.loaded) / (event.total || 100));
      case HttpEventType.Response:
        return 100;
      default:
        return 0;
    }
  }

  getEntities(): Observable<any[]> {
    return this.http.get<any[]>('/api/entites');
  }
}