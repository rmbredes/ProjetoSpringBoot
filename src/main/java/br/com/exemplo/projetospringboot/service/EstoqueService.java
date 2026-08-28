package br.com.exemplo.projetospringboot.service;

import br.com.exemplo.projetospringboot.entity.EventoProcessado;
import br.com.exemplo.projetospringboot.event.PedidoCriadoEvent;
import br.com.exemplo.projetospringboot.repository.EventoProcessadoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Serviço responsável pelas regras de processamento
 * de estoque relacionadas aos eventos de pedidos.
 *
 * Nesta fase, a operação de estoque ainda é simulada.
 * Porém, o controle de idempotência já é persistido no banco.
 */
@Service
public class EstoqueService {

    /*
     * Nome usado para identificar o processamento realizado
     * pelo consumer de estoque.
     */
    private static final String NOME_CONSUMER =
            "estoque-service-group";

    /*
     * Logger utilizado para registrar execuções normais
     * e eventos duplicados.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(EstoqueService.class);

    /*
     * Repository utilizado para verificar e registrar
     * os eventos já processados.
     */
    private final EventoProcessadoRepository eventoProcessadoRepository;

    /**
     * Recebe o repository responsável pelo controle
     * persistente da idempotência.
     *
     * @param eventoProcessadoRepository repository de eventos processados
     */
    public EstoqueService(
            EventoProcessadoRepository eventoProcessadoRepository
    ) {
        this.eventoProcessadoRepository =
                eventoProcessadoRepository;
    }

    /**
     * Processa um evento de pedido criado somente uma vez.
     *
     * A verificação, a ação de negócio e o registro do evento
     * ficam dentro da mesma transação de banco.
     *
     * @param evento evento recebido do Kafka
     * @return true quando o evento foi processado agora;
     *         false quando já havia sido processado
     */
    @Transactional
    public boolean processar(
            PedidoCriadoEvent evento
    ) {
        /*
         * Consulta se este consumer já processou o evento.
         */
        boolean jaProcessado =
                eventoProcessadoRepository
                        .contarPorEventoEConsumer(
                                evento.eventoId(),
                                NOME_CONSUMER
                        ) > 0;

        /*
         * Se o evento já estiver registrado, não repetimos
         * o efeito de negócio.
         */
        if (jaProcessado) {
            LOGGER.info(
                    "Evento duplicado ignorado pelo estoque: eventoId={}, pedidoId={}",
                    evento.eventoId(),
                    evento.pedidoId()
            );

            return false;
        }

        /*
         * Simula o efeito de negócio que futuramente poderá
         * alterar uma quantidade ou criar uma reserva real.
         */
        LOGGER.info(
                "SIMULAÇÃO: efeito de estoque executado para eventoId={}, pedidoId={}",
                evento.eventoId(),
                evento.pedidoId()
        );

        /*
         * Registra persistentemente que este consumer
         * concluiu o processamento do evento.
         */
        EventoProcessado eventoProcessado =
                new EventoProcessado(
                        evento.eventoId(),
                        NOME_CONSUMER,
                        Instant.now()
                );

        eventoProcessadoRepository.save(
                eventoProcessado
        );

        return true;
    }
}