import React from 'react';
import ReactDOM from 'react-dom/client';
import keycloak from './keycloak';
import './index.css';
import App from './App';
import { BrowserRouter } from 'react-router-dom';
import { ReactKeycloakProvider } from '@react-keycloak/web';

/* const keycloak = new Keycloak({
  url: 'http://localhost:9090/realms/home/protocol/openid-connect/auth', // TODO: Replace with your Keycloak server URL
  realm: 'home', // TODO: Replace with your realm
  clientId: 'mcpfileclient', // TODO: Replace with your client ID
}); */

const root = ReactDOM.createRoot(
  document.getElementById('root') as HTMLElement
);
root.render(
  <ReactKeycloakProvider
    authClient={keycloak}
    initOptions={{
      onLoad: 'check-sso', // changed from 'login-required'
      checkLoginIframe: false,
      redirectUri: window.location.origin,
    }}
  >
    <BrowserRouter>
      <App />
    </BrowserRouter>
  </ReactKeycloakProvider>
); 