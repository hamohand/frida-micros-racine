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
  public showTombes: boolean = false;
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
          const numPere = this.frida.calcul.numerateurPere || 0;
          const numMere = this.frida.calcul.numerateurMere || 0;

          this.denominateur_L = this.traductionArabeService.nombreVersArabe(this.frida.calcul.denominateur || 1);
          this.numerateurConjoint_L = this.traductionArabeService.nombreVersArabe(numConjoint);
          this.numerateurGarcons_L = this.traductionArabeService.nombreVersArabe(numGarcons);
          this.numerateurFilles_L = this.traductionArabeService.nombreVersArabe(numFilles);
          
          // Lancer le calcul de vérification complet (nécessite les héritiers)
          this.calculerNumerateurVerif();
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
          console.log("Conjoint -------- : ", this.heritiers[0]?.identite?.latines);
          console.log("data heritiers : ",this.heritiers);
          
          // Lancer le calcul de vérification complet (nécessite la frida)
          this.calculerNumerateurVerif();
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

  calculerNumerateurVerif(): void {
    if (!this.frida || !this.frida.calcul || !this.heritiers || this.heritiers.length === 0) {
      return;
    }
    
    let total = 0;
    for (const h of this.heritiers) {
      const part = this.getHeritierPart(h);
      if (part && part.num) {
        total += part.num;
      }
    }
    
    this.numerateurVerif = total;
    this.numerateurVerif_L = this.traductionArabeService.nombreVersArabe(this.numerateurVerif);
  }

  getNombreTombes(): number {
    let tombes = 1; // Le défunt principal
    if (this.heritiers && this.heritiers.length > 0) {
      // Petits-fils (09) ou petites-filles (10) impliquent un enfant pré-décédé
      const hasPetitsFils = this.heritiers.some((h: any) => h.numParente == 9 || h.numParente == 9);
      const hasPetitesFilles = this.heritiers.some((h: any) => h.numParente == 10);
      if (hasPetitsFils || hasPetitesFilles) {
        tombes += 1; // Au moins un enfant pré-décédé
      }
    }
    return tombes;
  }

  isMasculin(sexe: string | undefined): boolean {
    if (!sexe) return false;
    const s = sexe.trim();
    return s === 'ذكر' || s === 'ذ' || s.toUpperCase() === 'M';
  }

  getRelationArabe(heritier: any): string {
    const isMaleDefunt = this.isMasculin(this.frida?.defunt?.identite?.sexe);
    const numParente = heritier.numParente;
    const isMaleHeritier = this.isMasculin(heritier.identite?.sexe);

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
    } else if (numParente == 6) {
      return isMaleDefunt ? 'عمه' : 'عمها';
    } else if (numParente == 7) {
      return isMaleDefunt ? 'ابن عمه' : 'ابن عمها';
    } else if (numParente == 8) {
      return isMaleDefunt ? 'جده' : 'جدها';
    }
    return '';
  }

  getRelationArabePourPart(heritier: any): string {
    const isMaleDefunt = this.isMasculin(this.frida?.defunt?.identite?.sexe);
    const numParente = heritier.numParente;
    const isMaleHeritier = this.isMasculin(heritier.identite?.sexe);

    if (numParente == 5) {
      if (isMaleDefunt) return isMaleHeritier ? 'لأخيه' : 'لأخته';
      else return isMaleHeritier ? 'لأخيها' : 'لأختها';
    } else if (numParente == 6) {
      return isMaleDefunt ? 'لعمه' : 'لعمها';
    } else if (numParente == 7) {
      return isMaleDefunt ? 'لابن عمه' : 'لابن عمها';
    } else if (numParente == 8) {
      return isMaleDefunt ? 'لجده' : 'لجدها';
    }
    return '';
  }

  getTexteHeritier(heritier: any): string {
    const isMaleHeritier = this.isMasculin(heritier.identite?.sexe);
    
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
      if (this.isMasculin(heritier.identite?.sexe)) {
         return { num: this.frida.calcul.numerateurGarcons || 0, den: den };
      } else {
         return { num: this.frida.calcul.numerateurFilles || 0, den: den };
      }
    } else if (heritier.numParente == 4) {
      if (this.isMasculin(heritier.identite?.sexe)) {
         return { num: this.frida.calcul.numerateurPere || 0, den: den };
      } else {
         return { num: this.frida.calcul.numerateurMere || 0, den: den };
      }
    } else if (heritier.numParente == 5) {
      if (this.isMasculin(heritier.identite?.sexe)) {
         return { num: this.frida.calcul.numerateurFreres || 0, den: den };
      } else {
         return { num: this.frida.calcul.numerateurSoeurs || 0, den: den };
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
                  font-size: 12pt;
                  direction: rtl;
                  padding: 40px;
                  line-height: 1.8;
                }
                
                .header-officiel { text-align: center; font-weight: bold; font-size: 18pt; margin-bottom: 1.5cm; }
                .header-titre-principal { font-size: 24pt; text-decoration: underline; margin-bottom: 10px; }
                .vert { font-weight: bold; color: black; }
                .bold { font-weight: bold; }
                
                .section { margin-bottom: 1.5cm; text-align: right; }
                .liste-temoins, .liste-heritiers, .liste-parts { margin-right: 1cm; }
                
                .fraction-box {
                  display: flex;
                  flex-direction: column;
                  align-items: center;
                  justify-content: flex-end;
                  font-weight: bold;
                  margin: 0;
                }
                .fraction-box .num { border-bottom: 1px solid black; padding: 0 5px; line-height: 1.2; font-size: 14pt; }
                .fraction-box .den { padding: 0 5px; line-height: 1.2; font-size: 14pt; }
                
                .dash-paragraph { display: block; text-align: right; overflow: hidden; position: relative; margin-bottom: 5px; }
                .dash-paragraph .text-content { display: inline; position: relative; z-index: 2; padding-left: 10px; }
                .dash-paragraph .leader-spacer { display: inline-block; width: 0; height: 0; position: relative; }
                .dash-paragraph .leader-spacer::after { content: ""; position: absolute; right: 0; bottom: 8px; width: 21cm; border-bottom: 2px dashed black; z-index: 1; }

                .heir-paragraph { display: block; text-align: right; overflow: hidden; position: relative; margin-bottom: 15px; }
                .heir-paragraph .text-content { display: inline; position: relative; z-index: 2; padding-left: 10px; }
                .heir-paragraph .leader-spacer { display: inline-block; width: 0; height: 0; position: relative; }
                .heir-paragraph .leader-spacer::after { content: ""; position: absolute; right: 0; bottom: 8px; width: 21cm; border-bottom: 2px dashed black; z-index: 1; }
                .heir-paragraph .fraction-box { float: left; display: flex; flex-direction: column; align-items: center; background: white; -webkit-print-color-adjust: exact; print-color-adjust: exact; position: relative; z-index: 2; padding-right: 15px; margin: 0; }

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
