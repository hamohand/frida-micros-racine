import {AfterViewChecked, AfterViewInit, Component, ElementRef, Input, OnInit, ViewChild} from '@angular/core';
import {CommonModule} from "@angular/common";
import {ActivatedRoute} from "@angular/router";
import {FridaService} from "../../services/frida.service";
import {TraductionArabeService} from "../../services/traduction-arabe.service";
import {MatCardModule} from "@angular/material/card";
import {MatDividerModule} from "@angular/material/divider";
import {MatButtonModule} from "@angular/material/button";

@Component({
  selector: 'app-frida',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatDividerModule, MatButtonModule],
  templateUrl: './frida.component.html',
  styleUrl: './frida.component.css'
})
export class FridaComponent implements OnInit, AfterViewInit {
  public frida: any = null;
  //numFrida: string = '1956010320250116';
  //numFrida: string = '19560103202501171733';
  numFrida: string = '';
  url: string = 'http://localhost:8080/api/frida/';

  public heritiers: any = [];
  public temoins: any = [];
  public numerateurVerif: number = 0;
//
  public denominateur_L: string = '...chiffres en arabe...';
  public numerateurConjoint_L: string = '...chiffres en arabe...';
  public numerateurGarcons_L: string = '...chiffres en arabe...';
  public numerateurFilles_L: string = '...chiffres en arabe...';
  public numerateurVerif_L: string = '...chiffres en arabe...';
//
  constructor(
      private route: ActivatedRoute, //passage du paramètre 'numFrida'
      private fridaService: FridaService,
      private traductionArabeService: TraductionArabeService,
      //private aiBdService: AiBdService,
  ){}
  ngOnInit() {
    this.route.queryParams.subscribe((params) => {
      this.numFrida = params['numFrida'] ?? 'Aucune donnée reçue';
    });
   // this.numFrida='19560103202501171733'
    console.log("numFrida ngOnInit: ",this.numFrida);
    this.afficherFrida();
  }

  afficherFrida(): void {
    console.log("numFrida frida.ts 2: ",this.numFrida);
    // la frida par son numéro 2
    this.fridaParNum();
    // héritiers de la frida ordonnés
    this.afficheHeritiers();
    // Affiche les témoins de la frida
    this.afficheTemoins();

    // Déclencher l'impression seulement après le chargement des données
    setTimeout(() => {
      if (this.contenuFridaRef) {
        console.log('Données et contenu chargés, prêt pour impression.');
      }
    }, 2000); // Ajustez le délai si nécessaire
  }

  fridaParNum(): void {
    this.fridaService.lancerApi(this.url + this.numFrida).subscribe({
    //this.fridaService.lancerApi('http://localhost:8080/api/frida/1956010320250116').subscribe({
      next: data => {
        if (data && data.numFrida) {
          // Traitez les données comme attendu
          this.frida = data;
          console.log('numFrida: ', this.frida.numFrida);
          console.log("Défunt -------- : " , this.frida.defunt.extraitNaissance.latines);
          // Calculs
          this.denominateur_L = this.traductionArabeService.nombreVersArabe(this.frida.calcul.denominateur);
          this.numerateurConjoint_L = this.traductionArabeService.nombreVersArabe(this.frida.calcul.numerateurConjoint);
          this.numerateurGarcons_L = this.traductionArabeService.nombreVersArabe(this.frida.calcul.numerateurGarcons);
          this.numerateurFilles_L = this.traductionArabeService.nombreVersArabe(this.frida.calcul.numerateurFilles);
          //Vérification
          this.numerateurVerif = this.frida.calcul.numerateurConjoint * this.frida.calcul.nbConjoints
              + this.frida.calcul.numerateurGarcons * this.frida.calcul.nbGarcons
              + this.frida.calcul.numerateurFilles * this.frida.calcul.nbFilles;
          this.numerateurVerif_L = this.traductionArabeService.nombreVersArabe(this.numerateurVerif);
          console.log("Notaire : "+this.frida.notaire);
        } else {
          console.error('Données invalides reçues pour "frida" : ', data);
          this.frida = null; // Éviter une référence invalide.
        }

      },
      error: (err) => {
        console.error('FridaComponent : Erreur lors de la récupération de la frida:', err);
        // Gérer le cas d'une erreur réseau
      }
    });
  }

  //héritiers de la frida ordonnées selon une frida
  afficheHeritiers(): void {
    //héritiers de la frida  par le numéro de la frida ordonnés
    this.fridaService.lancerApi(this.url + 'listeHeritiers/' + this.numFrida).subscribe({
      next: data => {
        if (data){
          this.heritiers = data;
          console.log("Conjoint -------- : ", this.heritiers[0].extraitNaissance.latines);
          console.log("data heritiers : ",this.heritiers);
        } else {
          console.error('Données invalides reçues pour "heritiers" :', data);
          this.heritiers = null; // Éviter une référence invalide.
        }

      },
      error: err => console.log(err)
    });
  }

  //Affiche les témoins d'une frida
  afficheTemoins(): void {
    //héritiers de la frida  par le numéro de la frida
    this.fridaService.lancerApi(this.url + 'listeTemoins/' + this.numFrida).subscribe({
      next: data => {
        if (data) {
          this.temoins = data;
          console.log("this.Temoins -------- : ", this.temoins);
        } else {
          console.error('Données invalides reçues pour "temoins" :', data);
          this.temoins = null; // Éviter une référence invalide.
        }
      },
      error: err => console.log(err)
    });
  }

  affichageFrida(){
    console.log("numFrida frida.ts: ",this.numFrida);
  }

  /* -------------- IMPRESSION ----------------- */
  // Méthode pour imprimer toute la page
  // printFrida() {
  //   window.print();
  // }

  // Méthode pour imprimer uniquement le contenu du ng-container
  // Référence au 'div' via ViewChild
  @ViewChild('contenuFrida', { static: false }) contenuFridaRef!: ElementRef;

  ngAfterViewInit() {
    // Référence obtenue sur le div wrapper
    if (this.contenuFridaRef) {
      console.log('Référence wrapper récupérée avec succès :', this.contenuFridaRef);
    } else {
      console.error('La référence wrapper est introuvable.');
    }
  }

  printNgContainer() {
    if (!this.frida && this.temoins.length === 0 && this.heritiers.length === 0) {
      alert('Le contenu est vide, impossible d\'imprimer.');
      return;
    }

    if (this.contenuFridaRef) {
      const printContents = this.contenuFridaRef.nativeElement.innerHTML;
      const printWindow = window.open('', '_blank');
      if (printWindow) {
        printWindow.document.open();
        printWindow.document.write(`
          <html>
            <head>
              <title>Impression du contenu</title>
              <style>
                body {
                  font-family: Arial, sans-serif;
                  color: black;
                  font-size: 12pt;
                  direction: rtl;
                  padding: 60px;
                }
                mat-card {
                  border: 1px solid #ccc;
                  padding: 10px;
                  margin: 0;
                }
              </style>
            </head>
            <body onload="window.print(); window.close();">
              ${printContents}
            </body>
          </html>
        `);
        printWindow.document.close();
      }
    } else {
      console.error('Référence au ng-container introuvable.');
    }
  }

}
