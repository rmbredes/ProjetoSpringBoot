package br.com.exemplo.projetospringboot.integration;

import br.com.exemplo.projetospringboot.dto.AutorizarEntregaRequest;
import br.com.exemplo.projetospringboot.dto.EntregaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Centraliza a comunicação HTTP com o logistica-service.
 *
 * A chamada REST é protegida por um Circuit Breaker.
 * Quando a logística apresenta falhas repetidas, o circuito
 * é aberto e novas tentativas são bloqueadas temporariamente.
 */
@Component
public class LogisticaClient {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(LogisticaClient.class);

    private final RestClient restClient;
    private final CircuitBreaker circuitBreaker;

    /**
     * O Spring injeta automaticamente o CircuitBreakerFactory
     * criado pelo starter do Spring Cloud.
     *
     * A fábrica cria o circuito chamado "logisticaService".
     * Esse nome deve ser exatamente igual ao utilizado
     * posteriormente no application.yaml.
     */
    public LogisticaClient(
            @Value("${application.logistica.base-url}")
            String baseUrl,
            CircuitBreakerFactory<?, ?> circuitBreakerFactory
    ) {
        this.restClient = RestClient
                .builder()
                .baseUrl(baseUrl)
                .build();

        this.circuitBreaker =
                circuitBreakerFactory.create(
                        "logisticaService"
                );
    }

    /**
     * Executa a autorização protegida pelo Circuit Breaker.
     *
     * O primeiro argumento do run() contém a operação real.
     * O segundo argumento é executado quando:
     *
     * - a chamada HTTP falha; ou
     * - o circuito já está aberto.
     *
     * Não devolvemos uma entrega fictícia como fallback.
     * Lançamos uma exceção para manter o pedido PENDENTE.
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
     * Contém somente a chamada HTTP propriamente dita.
     *
     * Esse método fica separado para deixar evidente
     * qual operação está protegida pelo Circuit Breaker.
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

        EntregaResponse response = restClient
                .post()
                .uri("/entregas")
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