import { Component } from '@angular/core';
import {LireaiEcrirebdService} from "../../services/lireai-ecrirebd.service";

@Component({
    selector: 'app-lireaiEcrirebdComponent',
    standalone: true,
    template: `
        <button  [disabled]="isWriting">
            {{ isWriting ? "Écriture en cours..." : "Écrire dans la Base de Données" }}
        </button>
    `,

})
export class LireaiEcrirebdComponent {
    isWriting = false;

    constructor(private lireaiEcrirebdService: LireaiEcrirebdService) {}

    /*onLireAiEcrireBd(): void {
        this.isWriting = true;
        this.lireaiEcrirebdService.lireAiEcrireBd().subscribe({
            next: (response) => {
                console.log('Réponse du serveur :', response);
                this.isWriting = false;
            },
            error: (error) => {
                console.error('Erreur lors de l’écriture :', error);
                this.isWriting = false;
            },
        });
    }*/
}