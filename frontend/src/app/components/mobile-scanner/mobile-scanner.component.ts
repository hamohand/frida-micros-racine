import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-mobile-scanner',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './mobile-scanner.component.html',
  styleUrls: ['./mobile-scanner.component.scss']
})
export class MobileScannerComponent implements OnInit {
  sessionId: string = '';
  status: 'idle' | 'uploading' | 'success' | 'error' = 'idle';
  errorMessage: string = '';

  constructor(private route: ActivatedRoute, private http: HttpClient) {}

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      this.sessionId = params.get('sessionId') || '';
    });
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (!file) return;

    this.status = 'uploading';
    const reader = new FileReader();
    reader.onload = () => {
      const base64 = reader.result as string;
      
      this.http.post<any>('/api/pdfs/mobile-mrz', { 
        image: base64,
        sessionId: this.sessionId
      }).subscribe({
        next: () => {
          this.status = 'success';
        },
        error: (err) => {
          console.error(err);
          this.status = 'error';
          this.errorMessage = err.error?.message || err.message || "Erreur de transfert";
        }
      });
    };
    reader.readAsDataURL(file);
  }
}
