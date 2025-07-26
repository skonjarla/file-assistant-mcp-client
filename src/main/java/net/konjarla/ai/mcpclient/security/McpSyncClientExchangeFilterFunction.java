package net.konjarla.ai.mcpclient.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.ClientCredentialsOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizationContext;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class McpSyncClientExchangeFilterFunction implements ExchangeFilterFunction {

    private final ClientCredentialsOAuth2AuthorizedClientProvider clientCredentialTokenProvider = new ClientCredentialsOAuth2AuthorizedClientProvider();

    private final ClientRegistrationRepository clientRegistrationRepository;

    // Must match registration id in property
    // spring.security.oauth2.client.registration.<REGISTRATION-ID>.authorization-grant-type=client_credentials
    private static final String CLIENT_CREDENTIALS_CLIENT_REGISTRATION_ID = "authserver-client-credentials";

    public McpSyncClientExchangeFilterFunction(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest httpServletRequest = attributes.getRequest();
            String authorization = httpServletRequest.getHeader("Authorization").substring("Bearer ".length());
            log.debug("Adding access token to request: {}", authorization);
            ClientRequest requestWithToken = ClientRequest.from(request)
                    .headers(headers -> headers.setBearerAuth(authorization))
                    .build();
            log.debug("Request with token: {}", requestWithToken.headers().get("Authorization"));
            return next.exchange(requestWithToken);
            // return this.delegate.filter(request, next);
        }
        else {
            var accessToken = getClientCredentialsAccessToken();
            var requestWithToken = ClientRequest.from(request)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .build();
            return next.exchange(requestWithToken);
        }
    }

    private String getClientCredentialsAccessToken() {
        log.debug("Getting client credentials access token");
        var clientRegistration = this.clientRegistrationRepository
                .findByRegistrationId(CLIENT_CREDENTIALS_CLIENT_REGISTRATION_ID);

        var authRequest = OAuth2AuthorizationContext.withClientRegistration(clientRegistration)
                .principal(new AnonymousAuthenticationToken("client-credentials-client", "client-credentials-client",
                        AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")))
                .build();
        return this.clientCredentialTokenProvider.authorize(authRequest).getAccessToken().getTokenValue();
    }
}