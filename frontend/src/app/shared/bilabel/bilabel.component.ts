import { Component, HostBinding, Input, inject } from '@angular/core';
import { LanguageService } from '../../services/language.service';

@Component({
  selector: 'app-bilabel',
  standalone: true,
  template: `
    @if (langService.currentLangSignal() === 'fr') {
      <span class="bl-fr">{{ fr }}</span>
    } @else {
      <span class="bl-ar" dir="rtl">{{ ar || fr }}</span>
    }
  `,
  styles: [`
    :host {
      display: inline;
      white-space: nowrap;
    }
    :host.bl-stack {
      display: inline;
      white-space: normal;
    }
    .bl-ar {
      font-family: var(--font-arabic, 'Amiri', serif);
      font-size: 1.05em;
      unicode-bidi: isolate;
      direction: rtl;
    }
  `]
})
export class BilabelComponent {
  langService = inject(LanguageService);

  @Input() fr = '';
  @Input() ar = '';
  @Input() wrap = false;

  @HostBinding('class.bl-stack') get isStack() { return this.wrap; }
}
