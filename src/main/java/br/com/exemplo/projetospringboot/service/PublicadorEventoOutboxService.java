package br.com.exemplo.projetospringboot.service;

import br.com.exemplo.projetospringboot.entity.EventoOutbox;
import br.com.exemplo.projetospringboot.enums.StatusEventoOutbox;
import br.com.exemplo.projetospringboot.event.PedidoCriadoEvent;
import br.com.exemplo.projetospringboot.messaging.producer.PedidoProducer;
import br.com.exemplo.projetospringboot.observability.metrics.OutboxMetrics;
import br.com.exemplo.projetospringboot.repository.EventoOutboxRepository;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import br.com.exemplo.projetospringboot.observability.logging.EventoLogContext;
import org.slf4j.MDC;

/**
 * Serviço responsável por publicar no Kafka um evento
 * que está armazenado na tabela de Outbox.
 */
@Service
public class PublicadorEventoOutboxService {

    /**
     * Quantidade máxima de tentativas automáticas de publicação.
     */
    private static final int LIMITE_TENTATIVAS = 5;

    /**
     * Componente utilizado para registrar informações e erros no log.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PublicadorEventoOutboxService.class);

    /**
     * Repositório utilizado para consultar e atualizar a Outbox.
     */
    private final EventoOutboxRepository eventoOutboxRepository;

    /**
     * Producer responsável pela publicação do evento no Kafka.
     */
    private final PedidoProducer pedidoProducer;

    /**
     * Componente utilizado para transformar o JSON armazenado
     * novamente em um objeto Java.
     */
    private final ObjectMapper objectMapper;

    /**
     * Componente que registra as métricas de negócio da Outbox.
     */
    private final OutboxMetrics outboxMetrics;

    /**
     * Permite ativar temporariamente o span restaurado da Outbox.
     */
    private final Tracer tracer;

    /**
     * Reconstrói o contexto de tracing salvo no registro da Outbox.
     */
    private final Propagator propagator;

    /**
     * Recebe as dependências necessárias para publicar os eventos.
     *
     * @param eventoOutboxRepository repositório da Outbox
     * @param pedidoProducer producer responsável pelo Kafka
     * @param objectMapper componente responsável pelo JSON
     * @param outboxMetrics componente responsável pelas métricas da Outbox
     * @param tracer componente responsável pelos spans ativos
     * @param propagator componente responsável pela propagação do trace
     */
    public PublicadorEventoOutboxService(
            EventoOutboxRepository eventoOutboxRepository,
            PedidoProducer pedidoProducer,
            ObjectMapper objectMapper,
            OutboxMetrics outboxMetrics,
            Tracer tracer,
            Propagator propagator
    ) {
        this.eventoOutboxRepository = eventoOutboxRepository;
        this.pedidoProducer = pedidoProducer;
        this.objectMapper = objectMapper;
        this.outboxMetrics = outboxMetrics;
        this.tracer = tracer;
        this.propagator = propagator;
    }

    /**
     * Publica um evento pendente no Kafka.
     *
     * O registro é buscado novamente dentro desta transação.
     * Depois da confirmação do Kafka, ele é marcado como PUBLICADO.
     *
     * Se ocorrer uma falha, a quantidade de tentativas será
     * incrementada e o registro continuará disponível para retry.
     *
     * @param eventoOutboxId identificador do registro da Outbox
     */
    @Transactional
    public void publicar(
            Long eventoOutboxId
    ) {
        EventoOutbox eventoOutbox = eventoOutboxRepository
                .findById(eventoOutboxId)
                .orElseThrow();

        /*
         * Evita publicar novamente um evento que já tenha sido
         * concluído ou movido para o status ERRO.
         */
        if (eventoOutbox.getStatus() != StatusEventoOutbox.PENDENTE) {
            return;
        }

        /*
         * Cada registro restaura o contexto da requisição que o originou.
         * Isso é feito individualmente porque um lote pode reunir eventos
         * pertencentes a traces diferentes.
         */
        Span spanPublicacao =
                iniciarSpanPublicacao(eventoOutbox);

        /*
         * A execução do job ocorre em uma thread própria.
         *
         * Por isso, recuperamos o eventoId persistido na Outbox
         * e o associamos ao MDC da thread que publicará o evento.
         */
        try (
                Tracer.SpanInScope escopoTrace =
                        tracer.withSpan(spanPublicacao);

                MDC.MDCCloseable contextoEvento =
                        EventoLogContext.abrir(
                                eventoOutbox.getEventoId()
                        )
        ) {
            /*
             * Este try interno mantém o MDC disponível tanto durante
             * o processamento bem-sucedido quanto durante o tratamento
             * de uma eventual falha.
             */
            try {
                PedidoCriadoEvent evento =
                        converterParaEvento(
                                eventoOutbox.getPayload()
                        );

                pedidoProducer
                        .publicar(evento)
                        .join();

                eventoOutbox.marcarComoPublicado();

                Duration tempoAtePublicacao = Duration.between(
                        eventoOutbox.getCriadoEm(),
                        eventoOutbox.getPublicadoEm()
                );

                outboxMetrics.registrarTempoAtePublicacao(
                        tempoAtePublicacao
                );

                /*
                 * Não precisamos mais passar eventoId manualmente.
                 * O padrão de logging o recuperará automaticamente do MDC.
                 */
                LOGGER.info(
                        "Evento Outbox publicado: outboxId={}",
                        eventoOutbox.getId()
                );
            } catch (Exception exception) {
                spanPublicacao.error(exception);

                eventoOutbox.registrarFalha(
                        obterMensagemErro(exception),
                        LIMITE_TENTATIVAS
                );

                outboxMetrics.registrarFalhaPublicacao();

                /*
                 * Este log também terá eventoId porque o catch continua
                 * dentro do bloco em que o MDC está aberto.
                 */
                LOGGER.error(
                        "Falha ao publicar evento Outbox: outboxId={}, tentativa={}",
                        eventoOutbox.getId(),
                        eventoOutbox.getQuantidadeTentativas(),
                        exception
                );
            }
        } finally {
            spanPublicacao.end();
        }

        /*
         * Não precisamos chamar save novamente.
         *
         * Como a entidade foi encontrada dentro desta transação,
         * o dirty checking do JPA identificará as alterações e
         * executará o UPDATE ao finalizar a transação.
         */
    }

    /**
     * Restaura o contexto salvo na Outbox e cria um span para
     * a tentativa atual de publicação.
     *
     * Eventos antigos sem traceparent continuam sendo processados.
     * Nesse caso, o propagador utiliza o contexto atual do job.
     *
     * @param eventoOutbox evento que será publicado
     * @return span iniciado para a publicação
     */
    private Span iniciarSpanPublicacao(
            EventoOutbox eventoOutbox
    ) {
        Map<String, String> contexto = new HashMap<>();

        if (eventoOutbox.getTraceParent() != null) {
            contexto.put(
                    "traceparent",
                    eventoOutbox.getTraceParent()
            );
        }

        if (eventoOutbox.getTraceState() != null) {
            contexto.put(
                    "tracestate",
                    eventoOutbox.getTraceState()
            );
        }

        return propagator
                .extract(
                        contexto,
                        (origem, nome) ->
                                origem.get(nome)
                )
                .name("outbox.publicar")
                .tag(
                        "evento.id",
                        eventoOutbox.getEventoId().toString()
                )
                .tag(
                        "outbox.id",
                        String.valueOf(eventoOutbox.getId())
                )
                .tag(
                        "messaging.destination.name",
                        eventoOutbox.getTopico()
                )
                .start();
    }

    /**
     * Converte o JSON armazenado na Outbox novamente
     * para um evento de pedido criado.
     *
     * @param payload conteúdo JSON armazenado na Outbox
     * @return evento Java reconstruído
     * @throws JacksonException quando o JSON não puder ser convertido
     */
    private PedidoCriadoEvent converterParaEvento(
            String payload
    ) throws JacksonException {
        return objectMapper.readValue(
                payload,
                PedidoCriadoEvent.class
        );
    }

    /**
     * Obtém uma mensagem legível para registrar a falha no banco.
     *
     * @param exception falha ocorrida durante o processamento
     * @return mensagem que será armazenada na Outbox
     */
    private String obterMensagemErro(
            Exception exception
    ) {
        if (exception.getMessage() == null) {
            return exception.getClass().getSimpleName();
        }

        return exception.getMessage();
    }
}
