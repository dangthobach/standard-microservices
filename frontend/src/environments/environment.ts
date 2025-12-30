export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  keycloak: {
    issuer: 'http://localhost:8180/realms/enterprise',
    clientId: 'enterprise-frontend',
    redirectUri: window.location.origin,
    scope: 'openid profile email',
    responseType: 'code',
    usePkce: true,
    showDebugInformation: true,
    requireHttps: false
  },
  features: {
    enableTracing: true,
    enableMetrics: true
  }
};
