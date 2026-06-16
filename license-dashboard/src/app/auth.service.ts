import { Injectable } from '@angular/core';
import { AuthConfig, OAuthService } from 'angular-oauth2-oidc';

export const authConfig: AuthConfig = {
  issuer: 'https://auth.hscode.enclume-numerique.com/realms/frida-realm',
  redirectUri: window.location.origin + '/',
  clientId: 'frida-license-dashboard',
  responseType: 'code',
  scope: 'openid profile email',
  showDebugInformation: true,
  requireHttps: true
};

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  constructor(private oauthService: OAuthService) {
    this.oauthService.configure(authConfig);
    this.oauthService.setupAutomaticSilentRefresh();
    this.oauthService.loadDiscoveryDocumentAndTryLogin().then(() => {
      if (!this.oauthService.hasValidIdToken() || !this.oauthService.hasValidAccessToken()) {
        this.oauthService.initLoginFlow();
      }
    });
  }

  public get token() { return this.oauthService.getAccessToken(); }
  public get claims() { return this.oauthService.getIdentityClaims(); }
  
  public logout() {
    this.oauthService.logOut();
  }
}
