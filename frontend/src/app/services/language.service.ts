import { Injectable, Inject, PLATFORM_ID, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { isPlatformBrowser } from '@angular/common';

@Injectable({ providedIn: 'root' })
export class LanguageService {
  currentLangSignal = signal<string>('fr');

  constructor(
    private translate: TranslateService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.translate.onLangChange.subscribe((event) => {
      this.currentLangSignal.set(event.lang);
    });
  }

  init() {
    this.translate.addLangs(['fr', 'ar']);
    
    let defaultLang = 'fr';
    if (isPlatformBrowser(this.platformId)) {
      const savedLang = localStorage.getItem('app_lang');
      if (savedLang && ['fr', 'ar'].includes(savedLang)) {
        defaultLang = savedLang;
      }
    }
    
    this.translate.setFallbackLang('fr');
    this.setLanguage(defaultLang);
  }

  setLanguage(lang: string) {
    this.translate.use(lang);
    if (isPlatformBrowser(this.platformId)) {
      localStorage.setItem('app_lang', lang);
      this.updateDirection(lang);
    }
  }

  toggleLanguage() {
    const newLang = this.currentLangSignal() === 'fr' ? 'ar' : 'fr';
    this.setLanguage(newLang);
  }

  private updateDirection(lang: string) {
    const htmlTag = document.documentElement;
    if (lang === 'ar') {
      htmlTag.setAttribute('dir', 'rtl');
      htmlTag.setAttribute('lang', 'ar');
      document.body.classList.add('rtl-mode');
    } else {
      htmlTag.setAttribute('dir', 'ltr');
      htmlTag.setAttribute('lang', 'fr');
      document.body.classList.remove('rtl-mode');
    }
  }
  
  getCurrentLang(): string {
    return this.translate.getCurrentLang() || this.translate.getFallbackLang() || 'fr';
  }
}
