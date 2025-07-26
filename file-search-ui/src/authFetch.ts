import keycloak from './keycloak';

export async function authFetch(input: RequestInfo, init: RequestInit = {}) {
  // Ensure Keycloak is authenticated and token is available
  if (!keycloak.authenticated) {
    throw new Error('Not authenticated');
  }

  // Get the token (refresh if needed)
  const token = await keycloak.updateToken(5).then(() => keycloak.token);

  // Add Authorization header
  const headers = new Headers(init.headers || {});
  headers.set('Authorization', `Bearer ${token}`);

  return fetch(input, { ...init, headers });
} 