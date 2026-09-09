package br.com.exemplo.projetospringboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * Configura o componente responsável por obter e reutilizar
 * tokens OAuth2 utilizados nas chamadas entre aplicações.
 */
@Configuration
public class OAuth2ClientConfig {

    /**
     * Cria o gerenciador de clientes OAuth2.
     *
     * Ele:
     * - solicita o token ao Keycloak;
     * - mantém o token enquanto estiver válido;
     * - solicita outro quando for necessário.
     */
    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService
    ) {
        /*
         * Habilita especificamente o fluxo client_credentials,
         * utilizado na comunicação aplicação com aplicação.
         */
        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder
                        .builder()
                        .clientCredentials()
                        .build();

        /*
         * Cria o gerenciador usando as configurações declaradas
         * no application.yaml.
         */
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository,
                        authorizedClientService
                );

        manager.setAuthorizedClientProvider(
                authorizedClientProvider
        );

        return manager;
    }
}