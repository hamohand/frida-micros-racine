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
    if (n < 0) return "Nombre négatif non supporté";
    if (n === 0) return "صفر";
    if (n > 999999999) return "Nombre hors de portée (max 999 999 999)";

    const millions = Math.floor(n / 1000000);
    const resteMillions = n % 1000000;
    
    const milliers = Math.floor(resteMillions / 1000);
    const resteMilliers = resteMillions % 1000;

    let parties: string[] = [];

    if (millions > 0) {
        if (millions === 1) {
            parties.push("مليون");
        } else if (millions === 2) {
            parties.push("مليونين");
        } else if (millions >= 3 && millions <= 10) {
            parties.push(this.traduireMoinsDeMille(millions) + " ملايين");
        } else {
            parties.push(this.traduireMoinsDeMille(millions) + " مليون");
        }
    }

    if (milliers > 0) {
      if (milliers === 1) {
        parties.push("ألف");
      } else if (milliers === 2) {
        parties.push("ألفين");
      } else if (milliers >= 3 && milliers <= 10) {
        parties.push(this.traduireMoinsDeMille(milliers) + " آلاف");
      } else {
        parties.push(this.traduireMoinsDeMille(milliers) + " ألف");
      }
    }

    if (resteMilliers > 0) {
      parties.push(this.traduireMoinsDeMille(resteMilliers));
    }

    const resultatFinal = parties.join(" و ");
    console.log("Traduction : " + resultatFinal);
    return resultatFinal;
  }

  traduireMoinsDeMille(n: number): string {
    if (n === 0) return "";
    let resultat: string[] = [];
    const centaines = Math.floor(n / 100);
    const reste = n % 100;

    if (centaines === 1) {
      resultat.push("مائة");
    } else if (centaines === 2) {
      resultat.push("مائتين");
    } else if (centaines >= 3 && centaines <= 9) {
      const chiffres = ["", "", "", "ثلاث", "أربع", "خمس", "ست", "سبع", "ثمان", "تسع"];
      resultat.push(chiffres[centaines] + "مائة");
    }

    if (reste > 0) {
      if (reste < 10) {
        resultat.push(this.chiffresArabes[reste]);
      } else if (reste >= 11 && reste <= 19) {
        resultat.push(this.dizainesUniteArabes[reste - 10]);
      } else {
        const dizaines = Math.floor(reste / 10);
        const unites = reste % 10;
        if (unites > 0) {
          resultat.push(this.chiffresArabes[unites] + " و " + this.dizainesArabes[dizaines]);
        } else {
          resultat.push(this.dizainesArabes[dizaines]);
        }
      }
    }
    return resultat.join(" و ");
  }
}
