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
  url: string = '/api/frida/';

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
          console.log("Défunt -------- : " , this.frida.defunt.identite.latines);
          // Calculs
          // Calculs sécurisés (le backend peut renvoyer du null pour les parts inexistantes)
          const numConjoint = this.frida.calcul.numerateurConjoint || 0;
          const numGarcons = this.frida.calcul.numerateurGarcons || 0;
          const numFilles = this.frida.calcul.numerateurFilles || 0;

          this.denominateur_L = this.traductionArabeService.nombreVersArabe(this.frida.calcul.denominateur || 1);
          this.numerateurConjoint_L = this.traductionArabeService.nombreVersArabe(numConjoint);
          this.numerateurGarcons_L = this.traductionArabeService.nombreVersArabe(numGarcons);
          this.numerateurFilles_L = this.traductionArabeService.nombreVersArabe(numFilles);
          
          //Vérification
          this.numerateurVerif = numConjoint * (this.frida.calcul.nbConjoints || 0)
              + numGarcons * (this.frida.calcul.nbGarcons || 0)
              + numFilles * (this.frida.calcul.nbFilles || 0);
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
          console.log("Conjoint -------- : ", this.heritiers[0].identite.latines);
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

  getRelationArabe(heritier: any): string {
    const isMaleDefunt = this.frida?.defunt?.identite?.sexe === 'ذكر';
    const numParente = heritier.numParente;
    const isMaleHeritier = heritier.identite?.sexe === 'ذكر';

    if (numParente == 2) {
      return isMaleDefunt ? 'أرملته' : 'أرملها';
    } else if (numParente == 3) {
      if (isMaleDefunt) return isMaleHeritier ? 'ابنه' : 'بنته';
      else return isMaleHeritier ? 'ابنها' : 'بنتها';
    } else if (numParente == 4) {
      if (isMaleDefunt) return isMaleHeritier ? 'والده' : 'والدته';
      else return isMaleHeritier ? 'والدها' : 'والدتها';
    } else if (numParente == 5) {
      if (isMaleDefunt) return isMaleHeritier ? 'أخوه' : 'أخته';
      else return isMaleHeritier ? 'أخوها' : 'أختها';
    }
    return '';
  }

  getTexteHeritier(heritier: any): string {
    const isMaleHeritier = heritier.identite?.sexe === 'ذكر';
    
    // Nom et prénom tels quels (pas de formatage majuscule en arabe)
    const identite = `${heritier.identite?.nom || ''} ${heritier.identite?.prenom || ''}`;
    
    // Phrase de naissance et NIN avec accord du genre
    const ne_le = isMaleHeritier ? 'المولود في' : 'المولودة في';
    const titulaire_nin = isMaleHeritier ? 'الحامل للرقم الوطني' : 'الحاملة للرقم الوطني';
    
    return `${identite} ${ne_le} ${heritier.identite?.dateNaissance || ''} ${titulaire_nin} ${heritier.identite?.nin || ''}`;
  }

  getHeritierPart(heritier: any): {num: number, den: number} | null {
    if (!this.frida || !this.frida.calcul) return null;
    const den = this.frida.calcul.denominateur || 1;
    
    // Récupération de la part selon la parenté
    if (heritier.numParente == 2) {
      return { num: this.frida.calcul.numerateurConjoint || 0, den: den };
    } else if (heritier.numParente == 3) {
      if (heritier.identite?.sexe === 'ذكر') {
         return { num: this.frida.calcul.numerateurGarcons || 0, den: den };
      } else {
         return { num: this.frida.calcul.numerateurFilles || 0, den: den };
      }
    } else if (heritier.numParente == 4) {
      if (heritier.identite?.sexe === 'ذكر') {
         return { num: this.frida.calcul.numerateurPere || 0, den: den };
      } else {
         return { num: this.frida.calcul.numerateurMere || 0, den: den };
      }
    }
    
    // Ajouter frères/sœurs si leurs numérateurs sont ajoutés au backend plus tard
    // Fallback: reconstruire depuis le coefficient
    if (heritier.coefPart && heritier.coefPart > 0) {
      return { num: Math.round(heritier.coefPart * den), den: den };
    }
    
    return null;
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
              <title>Impression du document Frida</title>
              <style>
                @import url('https://fonts.googleapis.com/css2?family=Amiri:ital,wght@0,400;0,700;1,400;1,700&display=swap');
                
                body {
                  font-family: 'Amiri', serif;
                  color: black;
                  font-size: 16pt;
                  direction: rtl;
                  padding: 40px;
                  line-height: 1.8;
                }
                
                .header-officiel { text-align: center; font-weight: bold; font-size: 18pt; margin-bottom: 1.5cm; }
                .header-titre-principal { font-size: 24pt; text-decoration: underline; margin-bottom: 10px; }
                .vert { font-weight: bold; color: black; }
                .bold { font-weight: bold; }
                
                .section { margin-bottom: 1.5cm; text-align: justify; }
                .liste-temoins, .liste-heritiers, .liste-parts { margin-right: 1cm; }
                
                .fraction-box {
                  display: inline-flex;
                  flex-direction: column;
                  align-items: center;
                  vertical-align: middle;
                  font-weight: bold;
                  margin: 0 5px;
                }
                .fraction-box .num { border-bottom: 1px solid black; padding: 0 5px; line-height: 1.2; }
                .fraction-box .den { padding: 0 5px; line-height: 1.2; }
                
                .dash-fill-container { display: flex; align-items: center; width: 100%; margin-bottom: 5px; }
                .dash-fill-container .text-content { white-space: normal; }
                .dash-fill-container .dashes { flex-grow: 1; overflow: hidden; white-space: nowrap; margin: 0 10px; font-weight: bold; letter-spacing: 2px; transform: translateY(5px); }

                .signature-area {
                  margin-top: 2cm;
                  display: flex;
                  justify-content: flex-end;
                  font-weight: bold;
                  font-size: 18pt;
                }
                .signature-box { text-align: center; width: 6cm; }
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
