package br.com.exemplo.projetospringboot.messaging.job;

import br.com.exemplo.projetospringboot.entity.EventoOutbox;
import br.com.exemplo.projetospringboot.enums.StatusEventoOutbox;
import br.com.exemplo.projetospringboot.repository.EventoOutboxRepository;
import br.com.exemplo.projetospringboot.service.PublicadorEventoOutboxService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Job responsável por procurar eventos pendentes na tabela
 * de Outbox e solicitar sua publicação no Kafka.
 */
@Component
public class EventoOutboxJob {

    /**
     * Quantidade máxima de eventos processados em cada execução.
     *
     * O processamento em lotes evita carregar uma quantidade
     * muito grande de registros na memória.
     */
    private static final int TAMANHO_LOTE = 20;

    /**
     * Componente utilizado para registrar informações no log.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(EventoOutboxJob.class);

    /**
     * Repositório utilizado para buscar eventos pendentes.
     */
    private final EventoOutboxRepository eventoOutboxRepository;

    /**
     * Serviço responsável por publicar individualmente
     * cada evento encontrado.
     */
    private final PublicadorEventoOutboxService publicadorEventoOutboxService;

    /**
     * Recebe as dependências necessárias para executar o job.
     *
     * @param eventoOutboxRepository repositório da Outbox
     * @param publicadorEventoOutboxService serviço de publicação
     */
    public EventoOutboxJob(
            EventoOutboxRepository eventoOutboxRepository,
            PublicadorEventoOutboxService publicadorEventoOutboxService
    ) {
        this.eventoOutboxRepository = eventoOutboxRepository;
        this.publicadorEventoOutboxService =
                publicadorEventoOutboxService;
    }

    /**
     * Busca e processa um lote de eventos pendentes.
     *
     * O fixedDelay indica que o Spring aguardará cinco segundos
     * depois do término de uma execução para iniciar a próxima.
     *
     * O valor pode ser alterado futuramente pela propriedade
     * app.outbox.intervalo-ms sem modificar esta classe.
     */
    @Scheduled(
            fixedDelayString = "${app.outbox.intervalo-ms:5000}"
    )
    public void publicarEventosPendentes() {
        List<EventoOutbox> eventosPendentes =
                eventoOutboxRepository.buscarPorStatus(
                        StatusEventoOutbox.PENDENTE,
                        PageRequest.of(0, TAMANHO_LOTE)
                );

        if (eventosPendentes.isEmpty()) {
            return;
        }

        LOGGER.info(
                "Outbox: encontrados {} evento(s) pendente(s)",
                eventosPendentes.size()
        );

        /*
         * Cada evento é enviado separadamente ao serviço publicador.
         *
         * Como o publicador é outro bean do Spring, cada chamada
         * inicia sua própria transação.
         */
        for (EventoOutbox eventoOutbox : eventosPendentes) {
            publicadorEventoOutboxService.publicar(
                    eventoOutbox.getId()
            );
        }
    }
}