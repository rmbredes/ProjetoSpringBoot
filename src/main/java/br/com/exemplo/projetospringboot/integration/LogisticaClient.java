package br.com.exemplo.projetospringboot.integration;

import br.com.exemplo.projetospringboot.dto.AutorizarEntregaRequest;
import br.com.exemplo.projetospringboot.dto.EntregaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Centraliza a comunicação HTTP com o logistica-service.
 *
 * A classe agora possui duas proteções:
 *
 * - OAuth2/JWT: autentica o monólito no logistica-service;
 * - Circuit Breaker: protege o monólito quando a logística está indisponível.
 */
@Component
@ConditionalOnProperty(
        prefix = "application.logistica",
        name = "enabled",
        havingValue = "true"
)
public class LogisticaClient {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(LogisticaClient.class);

    /*
     * Nome da configuração declarada em:
     *
     * spring.security.oauth2.client.registration.logistica-keycloak
     */
    private static final String CLIENT_REGISTRATION_ID =
            "logistica-keycloak";

    /*
     * Identificação interna utilizada para armazenar
     * e reutilizar o cliente autorizado.
     *
     * Não representa um usuário humano.
     */
    private static final String CLIENT_PRINCIPAL =
            "projeto-springboot";

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;
    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public LogisticaClient(
            @Value("${application.logistica.base-url}")
            String baseUrl,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory,
            OAuth2AuthorizedClientManager authorizedClientManager
    ) {
        this.restClient = RestClient
                .builder()
                .baseUrl(baseUrl)
                .build();

        this.circuitBreaker =
                circuitBreakerFactory.create(
                        "logisticaService"
                );

        this.authorizedClientManager =
                authorizedClientManager;
    }

    /**
     * Executa a autorização protegida pelo Circuit Breaker.
     */
    public EntregaResponse autorizar(
            AutorizarEntregaRequest request
    ) {
        return circuitBreaker.run(
                () -> executarChamadaHttp(request),

                falha -> {
                    LOGGER.warn(
                            "Chamada à logística falhou ou foi bloqueada: " +
                                    "pedidoId={}, tipoFalha={}, mensagem={}",
                            request.pedidoId(),
                            falha.getClass().getSimpleName(),
                            falha.getMessage()
                    );

                    throw new LogisticaIndisponivelException(
                            "Não foi possível acessar o logistica-service",
                            falha
                    );
                }
        );
    }

    /**
     * Solicita ou recupera um token OAuth2 válido.
     *
     * O gerenciador não solicita necessariamente um novo token
     * em todas as chamadas. Enquanto o token atual estiver válido,
     * ele poderá ser reutilizado.
     */
    private String obterAccessToken() {
        OAuth2AuthorizeRequest authorizeRequest =
                OAuth2AuthorizeRequest
                        .withClientRegistrationId(
                                CLIENT_REGISTRATION_ID
                        )
                        .principal(CLIENT_PRINCIPAL)
                        .build();

        OAuth2AuthorizedClient authorizedClient =
                authorizedClientManager.authorize(
                        authorizeRequest
                );

        if (authorizedClient == null) {
            throw new IllegalStateException(
                    "O Keycloak não autorizou o cliente da logística"
            );
        }

        return authorizedClient
                .getAccessToken()
                .getTokenValue();
    }

    /**
     * Realiza a chamada HTTP ao logistica-service.
     */
    private EntregaResponse executarChamadaHttp(
            AutorizarEntregaRequest request
    ) {
        LOGGER.info(
                "Solicitando autorização logística: " +
                        "pagamentoId={}, pedidoId={}",
                request.pagamentoId(),
                request.pedidoId()
        );

        /*
         * Obtém um token válido antes da chamada.
         */
        String accessToken = obterAccessToken();

        EntregaResponse response = restClient
                .post()
                .uri("/entregas")

                /*
                 * Produz o cabeçalho:
                 *
                 * Authorization: Bearer eyJ...
                 */
                .headers(headers ->
                        headers.setBearerAuth(accessToken)
                )

                .body(request)
                .retrieve()
                .body(EntregaResponse.class);

        if (response == null) {
            throw new IllegalStateException(
                    "O logistica-service devolveu uma resposta vazia"
            );
        }

        LOGGER.info(
                "Autorização logística recebida: " +
                        "entregaId={}, pedidoId={}, status={}",
                response.id(),
                response.pedidoId(),
                response.status()
        );

        return response;
    }
}