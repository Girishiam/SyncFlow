// Production build: the API is expected on the same origin (e.g. behind a reverse proxy at /api).
export const environment = {
  production: true,
  apiBaseUrl: '/api',
  wsUrl: '/api/ws'
};
