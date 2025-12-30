export const environment = {
  production: true,
  apiUrl: 'https://api.enterprise.com/api',
  keycloak: {
    issuer: 'https://keycloak.enterprise.com/realms/enterprise',
    clientId: 'enterprise-frontend',
    redirectUri: window.location.origin,
    scope: 'openid profile email',
    responseType: 'code',
    usePkce: true,
    showDebugInformation: false,
    requireHttps: true
  },
  features: {
    enableTracing: true,
    enableMetrics: true
  }
};
