import { User, UserManager, WebStorageStateStore } from 'oidc-client-ts';

const OIDC_CONFIG = {
  authority: process.env.REACT_APP_AUTH_URL || 'http://localhost:8080/realms/enterprise',
  client_id: process.env.REACT_APP_CLIENT_ID || 'admin-ui',
  redirect_uri: window.location.origin + '/callback',
  post_logout_redirect_uri: window.location.origin,
  response_type: 'code',
  scope: 'openid profile email offline_access',
  userStore: new WebStorageStateStore({ store: window.localStorage }),
};

class AuthService {
  private userManager: UserManager;

  constructor() {
    this.userManager = new UserManager(OIDC_CONFIG);
  }

  public async getUser(): Promise<User | null> {
    return this.userManager.getUser();
  }

  public async login(): Promise<void> {
    return this.userManager.signinRedirect();
  }

  public async logout(): Promise<void> {
    return this.userManager.signoutRedirect();
  }

  public async handleCallback(): Promise<User> {
    return this.userManager.signinRedirectCallback();
  }

  public async getAccessToken(): Promise<string | null> {
    const user = await this.getUser();
    return user?.access_token || null;
  }
  
  public async isAuthenticated(): Promise<boolean> {
      const user = await this.getUser();
      return !!user && !user.expired;
  }
}

export default new AuthService();
