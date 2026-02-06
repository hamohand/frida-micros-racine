import { Component, OnInit } from '@angular/core';
import { FridaService } from '../../services/frida.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-frida-list',
  standalone: true,
  imports: [CommonModule],
  template: `
    <h3>&nbsp;</h3><h3>&nbsp;</h3>
    <h2>Liste des Fridas</h2>
    <table class="table">
      <thead>
        <tr>
          <th>Numéro Frida</th><th>Nom prénom du défunt</th><th>Date création frida</th>
<!--          <th>Date Naissance du défunt</th>-->
        </tr>
      </thead>
      <tbody style="text-align: center">
        <tr *ngFor="let frida of fridaList">
          <td>{{ frida?.numFrida }}</td>
          <td>{{ frida?.nom }}</td>
          <td>{{ frida?.dateCreation }}</td>
<!--          <td>{{ frida?.dateNaissane }}</td>-->
          
        </tr>
      </tbody>
    </table>
  `,
  styles: [`
    .table {
      margin: 50px;
      width: 90%;
      border-collapse: collapse;
    }
    .table th, .table td {
      border: 1px solid #ddd;
      padding: 8px;
      text-align: center;
    }
    .table th{
      color: springgreen;
    }
  `]
})
export class FridaListComponent implements OnInit {
  fridaList: { numFrida: string; dateCreation: string; dateNaissane: string; nom: string }[] = [];
  url: string = 'http://localhost:8080/api/frida/';

  constructor(private fridaService: FridaService) { }

  ngOnInit(): void {
    // this.fridaService.getFridaList()
    //     .subscribe((data) => {
    //       this.fridaList = data;
    //       console.log("fridaList : ",this.fridaList);
    //     });
    this.fridaService.lancerApi(this.url + "fridas").subscribe({
      next: data => {
        if (data && Array.isArray(data)) {
          this.fridaList = data.filter(f => f != null);
          console.log("fridaList : ", this.fridaList);
        }
      }
    })
  }
}
