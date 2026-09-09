package br.com.exemplo.projetospringboot.messaging.job;

import br.com.exemplo.projetospringboot.dto.AutorizarEntregaRequest;
import br.com.exemplo.projetospringboot.dto.EntregaResponse;
import br.com.exemplo.projetospringboot.entity.Pedido;
import br.com.exemplo.projetospringboot.integration.LogisticaClient;
import br.com.exemplo.projetospringboot.service.IntegracaoLogisticaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import br.com.exemplo.projetospringboot.integration.LogisticaIndisponivelException;

import java.util.List;

/**
 * Procura pedidos aprovados e chama o logistica-service.
 */
@Component
@ConditionalOnProperty(
        prefix = "application.logistica",
        name = "enabled",
        havingValue = "true"
)
public class IntegracaoLogisticaJob {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    IntegracaoLogisticaJob.class
            );

    private final IntegracaoLogisticaService integracaoLogisticaService;
    private final LogisticaClient logisticaClient;

    public IntegracaoLogisticaJob(
            IntegracaoLogisticaService integracaoLogisticaService,
            LogisticaClient logisticaClient
    ) {
        this.integracaoLogisticaService =
                integracaoLogisticaService;

        this.logisticaClient =
                logisticaClient;
    }

    /**
     * Executa depois do intervalo configurado, contado a partir
     * do término da execução anterior.
     */
    @Scheduled(
            fixedDelayString =
                    "${application.logistica.polling-delay-ms}"
    )
    public void processarPendentes() {
        List<Pedido> pendentes =
                integracaoLogisticaService.buscarPendentes();

        for (Pedido pedido : pendentes) {
            processarPedido(pedido);
        }
    }

    /**
     * A chamada REST é síncrona, mas acontece em uma thread
     * de job, fora da requisição original do usuário.
     */
    private void processarPedido(
            Pedido pedido
    ) {
        try {
            AutorizarEntregaRequest request =
                    new AutorizarEntregaRequest(
                            pedido.getPagamentoEventoId(),
                            pedido.getPagamentoId(),
                            pedido.getId()
                    );

            EntregaResponse response =
                    logisticaClient.autorizar(request);

            /*
             * No caminho feliz, a logística deve confirmar
             * exatamente o mesmo pedido solicitado.
             */
            if (!pedido.getId().equals(response.pedidoId())) {
                throw new IllegalStateException(
                        "A logística respondeu um pedido diferente"
                );
            }

            integracaoLogisticaService.confirmar(
                    pedido.getId()
            );

            LOGGER.info(
                    "Integração logística concluída: " +
                            "pedidoId={}, entregaId={}",
                    pedido.getId(),
                    response.id()
            );
        } catch (LogisticaIndisponivelException exception) {
            /*
             * A falha já foi identificada pelo LogisticaClient.
             *
             * Não imprimimos uma stack trace enorme a cada execução.
             * O pedido continua PENDENTE e poderá ser retomado
             * quando o Circuit Breaker permitir uma nova chamada.
             */
            LOGGER.warn(
                    "Integração logística temporariamente indisponível: " +
                            "pedidoId={}, statusLogistica={}",
                    pedido.getId(),
                    pedido.getStatusLogistica()
            );
        }
    }
}