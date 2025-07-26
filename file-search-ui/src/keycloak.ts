import Keycloak from 'keycloak-js';

const keycloak = new Keycloak({
  url: 'http://localhost:9090/',
  realm: 'home',
  clientId: 'mcpfileclient',
});

export default keycloak;