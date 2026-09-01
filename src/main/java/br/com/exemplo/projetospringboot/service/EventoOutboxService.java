package br.com.exemplo.projetospringboot.service;

import br.com.exemplo.projetospringboot.entity.EventoOutbox;
import br.com.exemplo.projetospringboot.event.PedidoCriadoEvent;
import br.com.exemplo.projetospringboot.repository.EventoOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;

import java.util.HashMap;
import java.util.Map;

/**
 * Serviço responsável por registrar eventos na tabela de Outbox.
 *
 * Ele não publica diretamente no Kafka. Sua responsabilidade
 * é apenas preparar e salvar o evento no banco de dados.
 */
@Service
public class EventoOutboxService {

    /**
     * Permite acessar o span que está ativo na thread atual.
     */
    private final Tracer tracer;

    /**
     * Converte o contexto do trace para headers padronizados
     * e, posteriormente, faz o caminho inverso.
     */
    private final Propagator propagator;
    /**
     * Logger utilizado para acompanhar o registro dos eventos.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(EventoOutboxService.class);

    /**
     * Registro utilizado para criar a observação da Outbox.
     */
    private final ObservationRegistry observationRegistry;

    /**
     * Nome do tópico que receberá os eventos de pedidos criados.
     */
    private static final String TOPICO_PEDIDOS_CRIADOS =
            "pedidos-criados";

    /**
     * Repositório utilizado para salvar os registros da Outbox.
     */
    private final EventoOutboxRepository eventoOutboxRepository;

    /**
     * Componente utilizado para converter objetos Java em JSON.
     */
    private final ObjectMapper objectMapper;

    /**
     * Recebe as dependências necessárias para registrar eventos.
     *
     * @param eventoOutboxRepository repositório da tabela de Outbox
     * @param objectMapper componente utilizado para gerar o JSON
     */
    public EventoOutboxService(
            Tracer tracer, Propagator propagator, ObservationRegistry observationRegistry, EventoOutboxRepository eventoOutboxRepository,
            ObjectMapper objectMapper
    ) {
        this.tracer = tracer;
        this.propagator = propagator;
        this.observationRegistry = observationRegistry;
        this.eventoOutboxRepository = eventoOutboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Registra um evento de pedido criado na tabela de Outbox.
     *
     * A propagação MANDATORY exige que já exista uma transação aberta.
     * Dessa maneira, este método deverá ser chamado pela transação que
     * também está salvando o pedido.
     *
     * Se o pedido ou o evento falhar, toda a transação será desfeita.
     *
     * @param evento evento de pedido criado que será armazenado
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarPedidoCriado(
            PedidoCriadoEvent evento
    ) {
        Observation
                .createNotStarted(
                        "outbox.registrar",
                        observationRegistry
                )
                // Identificadores únicos ficam somente no trace.
                //
                // Não são adicionados às métricas porque produziriam
                // uma quantidade muito grande de combinações.
                .highCardinalityKeyValue(
                        "evento.id",
                        evento.eventoId().toString()
                )
                .highCardinalityKeyValue(
                        "pedido.id",
                        evento.pedidoId().toString()
                )
                .observe(
                        () -> executarRegistro(evento)
                );
    }
    /**
     * Converte e persiste o evento dentro da observação ativa.
     *
     * @param evento evento que será registrado
     */
    private void executarRegistro(
            PedidoCriadoEvent evento
    ) {
        Map<String, String> contextoTrace =
                capturarContextoTrace();

        String payload = converterParaJson(evento);

        EventoOutbox eventoOutbox = new EventoOutbox(
                evento.eventoId(),
                PedidoCriadoEvent.class.getSimpleName(),
                TOPICO_PEDIDOS_CRIADOS,
                evento.pedidoId().toString(),
                payload,
                contextoTrace.get("traceparent"),
                contextoTrace.get("tracestate")
        );

        eventoOutboxRepository.save(eventoOutbox);

        LOGGER.info(
                "Evento registrado na Outbox: eventoId={}, pedidoId={}",
                evento.eventoId(),
                evento.pedidoId()
        );
    }

    /**
     * Converte o contexto técnico do span atual
     * em headers de propagação W3C.
     *
     * @return headers capturados ou mapa vazio quando não houver span
     */
    private Map<String, String> capturarContextoTrace() {
        Span spanAtual = tracer.currentSpan();

        if (spanAtual == null) {
            return Map.of();
        }

        Map<String, String> contexto = new HashMap<>();

        propagator.inject(
                spanAtual.context(),
                contexto,
                (destino, nome, valor) ->
                        destino.put(nome, valor)
        );

        return contexto;
    }
    /**
     * Converte o evento Java em uma representação JSON.
     *
     * Se a conversão falhar, uma exceção de execução será lançada.
     * Isso fará com que a transação atual seja desfeita, evitando
     * salvar o pedido sem o respectivo evento na Outbox.
     *
     * @param evento evento que será convertido
     * @return conteúdo JSON do evento
     */
    private String converterParaJson(
            PedidoCriadoEvent evento
    ) {
        try {
            return objectMapper.writeValueAsString(evento);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Não foi possível converter o evento de pedido para JSON",
                    exception
            );
        }
    }
}