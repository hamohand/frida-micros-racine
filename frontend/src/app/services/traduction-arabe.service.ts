import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class TraductionArabeService {

  constructor() { }
  private nombre: number = 0;

  // Méthode de configuration de l'URL (passe l'API URL ici)
  setNombre(nombre: number): number {
    this.nombre = nombre;
    return nombre;
  }

  //Nombres en arabe-------------------------------------------------
  chiffresArabes: string[] = [
    "صفر",  // 0
    "واحد", // 1
    "اثنان", // 2
    "ثلاثة", // 3
    "أربعة", // 4
    "خمسة", // 5
    "ستة",   // 6
    "سبعة",  // 7
    "ثمانية", // 8
    "تسعة"  // 9
  ];

  dizainesArabes: string[] = [
    "",        // 0
    "عشرة",    // 10
    "عشرون",   // 20
    "ثلاثون",  // 30
    "أربعون",  // 40
    "خمسون",   // 50
    "ستون",    // 60
    "سبعون",   // 70
    "ثمانون",  // 80
    "تسعون"   // 90
  ];

  dizainesUniteArabes: string[] = [
    "",            // 0
    "أحد عشر",     // 11
    "اثنا عشر",    // 12
    "ثلاثة عشر",   // 13
    "أربعة عشر",   // 14
    "خمسة عشر",    // 15
    "ستة عشر",     // 16
    "سبعة عشر",    // 17
    "ثمانية عشر",  // 18
    "تسعة عشر"     // 19
  ];

  // Fonction principale pour traduire un nombre

  nombreVersArabe(n: number): string {
    //let n: number = this.setNombre(this.nombre);
    if (n < 0 || n > 999) {
      return "Nombre hors de portée. Veuillez entrer un nombre entre 0 et 999.";
    }

    if (n === 0) {
      return (this.chiffresArabes)[0]; // si n est 0
    }

    let resultat: string[] = [];

    // Gérer les centaines
    const centaines = Math.floor(n / 100);
    if (centaines > 0) {
      if (centaines === 1) {
        resultat.push("مائة");
      } else if (centaines === 2) {
        resultat.push("مائتين");
      } else {
        resultat.push((this.chiffresArabes)[centaines] + " مائة");
      }
    }

    // Gérer les dizaines et les unités
    const reste = n % 100;
    if (reste > 0) {
      if (reste < 10) {
        resultat.push((this.chiffresArabes)[reste]);
      } else if (reste >= 11 && reste <= 19) {
        resultat.push((this.dizainesUniteArabes)[reste - 10]);
      } else {
        const dizaines = Math.floor(reste / 10);
        const unites = reste % 10;

        if (unites > 0) {
          resultat.push((this.chiffresArabes)[unites] + " و " + (this.dizainesArabes)[dizaines]);
        } else {
          resultat.push((this.dizainesArabes)[dizaines]);
        }
      }
    }
    console.log("Traduction : " + resultat.join(" "));
    return resultat.join(" ");
  }
}
