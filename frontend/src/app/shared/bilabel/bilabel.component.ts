import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-bilabel',
  standalone: true,
  template: `
    <span class="bl-fr">{{ fr }}</span>
    @if (ar) {
      <span class="bl-sep" aria-hidden="true"> · </span>
      <span class="bl-ar" dir="rtl">{{ ar }}</span>
    }
  `,
  styles: [`
    :host {
      display: inline;
      white-space: nowrap;
    }
    .bl-sep {
      color: var(--text-muted, #7f9c88);
      padding: 0 0.15em;
    }
    .bl-ar {
      font-family: var(--font-arabic, 'Amiri', serif);
      font-size: 0.97em;
      unicode-bidi: isolate;
      direction: rtl;
    }
  `]
})
export class BilabelComponent {
  @Input() fr = '';
  @Input() ar = '';
}
