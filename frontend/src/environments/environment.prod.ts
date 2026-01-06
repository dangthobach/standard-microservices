export const environment = {
  production: true,
  apiUrl: '/api',
  keycloak: {
    issuer: 'http://localhost:8180/realms/enterprise',
    clientId: 'microservices',
    redirectUri: window.location.origin + '/',
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
