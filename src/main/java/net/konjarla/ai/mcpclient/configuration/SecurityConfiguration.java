package net.konjarla.ai.mcpclient.configuration;

import net.konjarla.ai.mcpclient.security.McpSyncClientExchangeFilterFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class SecurityConfiguration {
    @Bean
    WebClient.Builder webClientBuilder(McpSyncClientExchangeFilterFunction filterFunction) {
        return WebClient.builder().filter(filterFunction);
        //return WebClient.builder().apply(filterFunction.configuration());
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .oauth2Client(Customizer.withDefaults())
                .csrf(CsrfConfigurer::disable)
                .build();
    }
}
