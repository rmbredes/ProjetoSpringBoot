package br.com.exemplo.projetospringboot.service;

import br.com.exemplo.projetospringboot.entity.EventoOutbox;
import br.com.exemplo.projetospringboot.enums.StatusEventoOutbox;
import br.com.exemplo.projetospringboot.event.PedidoCriadoEvent;
import br.com.exemplo.projetospringboot.messaging.producer.PedidoProducer;
import br.com.exemplo.projetospringboot.observability.metrics.OutboxMetrics;
import br.com.exemplo.projetospringboot.repository.EventoOutboxRepository;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.exemplo.projetospringboot.observability.logging.EventoLogContext;
import org.junit.jupiter.api.AfterEach;
import org.slf4j.MDC;

/**
 * Testes unitários do serviço responsável pela publicação
 * dos eventos armazenados na Outbox.
 */
@ExtendWith(MockitoExtension.class)
class PublicadorEventoOutboxServiceTest {

    /**
     * Simula o repositório da Outbox.
     */
    @Mock
    private EventoOutboxRepository eventoOutboxRepository;

    /**
     * Simula o producer responsável pelo envio ao Kafka.
     */
    @Mock
    private PedidoProducer pedidoProducer;

    /**
     * Simula o componente responsável pela conversão do JSON.
     */
    @Mock
    private ObjectMapper objectMapper;

    /**
     * Simula o componente responsável pelas métricas da Outbox.
     */
    @Mock
    private OutboxMetrics outboxMetrics;

    /** Serviço testado com tracing no-op nos testes unitários. */
    private PublicadorEventoOutboxService publicadorEventoOutboxService;

    /**
     * Cria o serviço com as dependências simuladas e componentes
     * de tracing no-op, pois estes testes verificam a regra da Outbox.
     */
    @BeforeEach
    void configurarServico() {
        publicadorEventoOutboxService =
                new PublicadorEventoOutboxService(
                        eventoOutboxRepository,
                        pedidoProducer,
                        objectMapper,
                        outboxMetrics,
                        Tracer.NOOP,
                        Propagator.NOOP
                );
    }


    /**
     * Garante que nenhum teste deixe dados no MDC
     * para o próximo teste executado na mesma thread.
     */
    @AfterEach
    void limparMdc() {
        MDC.clear();
    }


    /**
     * Verifica se um evento pendente é enviado ao Kafka
     * e marcado como publicado depois da confirmação.
     *
     * @throws JacksonException caso o mock da conversão JSON falhe
     */
    @Test
    void devePublicarEventoPendente()
            throws JacksonException {

        UUID eventoId = UUID.randomUUID();

        PedidoCriadoEvent evento = new PedidoCriadoEvent(
                eventoId,
                10L,
                1L,
                new BigDecimal("150.00"),
                Instant.now()
        );

        String payload = """
                {
                  "eventoId": "%s",
                  "pedidoId": 10,
                  "clienteId": 1,
                  "valor": 150.00
                }
                """.formatted(eventoId);

        EventoOutbox eventoOutbox = new EventoOutbox(
                eventoId,
                PedidoCriadoEvent.class.getSimpleName(),
                "pedidos-criados",
                "10",
                payload
        );

        /*
         * Simula que o registro pendente foi encontrado no banco.
         */
        when(
                eventoOutboxRepository.findById(1L)
        ).thenReturn(
                Optional.of(eventoOutbox)
        );

        /*
         * Simula a conversão do JSON armazenado para o evento Java.
         */
        when(
                objectMapper.readValue(
                        payload,
                        PedidoCriadoEvent.class
                )
        ).thenReturn(evento);

        /*
         * Simula a confirmação do Kafka e verifica o MDC
         * exatamente durante a chamada feita ao producer.
         */
        when(
                pedidoProducer.publicar(evento)
        ).thenAnswer(invocacao -> {
            /*
             * Neste momento ainda estamos dentro do contexto
             * aberto pelo PublicadorEventoOutboxService.
             */
            assertEquals(
                    eventoId.toString(),
                    MDC.get(
                            EventoLogContext.CHAVE_EVENTO_ID
                    )
            );

            return CompletableFuture.completedFuture(null);
        });

        publicadorEventoOutboxService.publicar(1L);

        /*
         * Após o método terminar, o try-with-resources deve
         * remover automaticamente o eventoId da thread.
         */
        assertNull(
                MDC.get(
                        EventoLogContext.CHAVE_EVENTO_ID
                )
        );
        /*
         * Confirma que o registro foi alterado depois
         * da resposta bem-sucedida do Kafka.
         */
        assertEquals(
                StatusEventoOutbox.PUBLICADO,
                eventoOutbox.getStatus()
        );

        assertNotNull(
                eventoOutbox.getPublicadoEm()
        );

        assertEquals(
                0,
                eventoOutbox.getQuantidadeTentativas()
        );

        assertNull(
                eventoOutbox.getUltimoErro()
        );

        /*
         * Confirma que o evento correto foi enviado ao producer.
         */
        verify(
                pedidoProducer
        ).publicar(evento);

        /*
         * Uma publicação confirmada deve registrar o período
         * durante o qual o evento permaneceu na Outbox.
         */
        verify(
                outboxMetrics
        ).registrarTempoAtePublicacao(
                any(Duration.class)
        );

        /*
         * Uma publicação bem-sucedida não deve incrementar
         * o contador de tentativas que falharam.
         */
        verify(
                outboxMetrics,
                never()
        ).registrarFalhaPublicacao();
    }

    /**
     * Verifica se uma falha na publicação mantém o evento pendente
     * e incrementa sua quantidade de tentativas.
     *
     * @throws JacksonException caso o mock da conversão JSON falhe
     */
    @Test
    void deveRegistrarFalhaQuandoKafkaNaoConfirmar()
            throws JacksonException {

        UUID eventoId = UUID.randomUUID();

        PedidoCriadoEvent evento = new PedidoCriadoEvent(
                eventoId,
                20L,
                1L,
                new BigDecimal("200.00"),
                Instant.now()
        );

        String payload = """
            {
              "eventoId": "%s",
              "pedidoId": 20,
              "clienteId": 1,
              "valor": 200.00
            }
            """.formatted(eventoId);

        EventoOutbox eventoOutbox = new EventoOutbox(
                eventoId,
                PedidoCriadoEvent.class.getSimpleName(),
                "pedidos-criados",
                "20",
                payload
        );

        /*
         * Simula que o evento pendente foi encontrado no banco.
         */
        when(
                eventoOutboxRepository.findById(2L)
        ).thenReturn(
                Optional.of(eventoOutbox)
        );

        /*
         * Simula a conversão do JSON para o evento Java.
         */
        when(
                objectMapper.readValue(
                        payload,
                        PedidoCriadoEvent.class
                )
        ).thenReturn(evento);

        /*
         * Simula uma resposta futura que terminou com erro.
         *
         * O failedFuture representa uma operação assíncrona
         * que não conseguiu ser concluída.
         */
        when(
                pedidoProducer.publicar(evento)
        ).thenReturn(
                CompletableFuture.failedFuture(
                        new IllegalStateException(
                                "Kafka indisponível"
                        )
                )

        );

        publicadorEventoOutboxService.publicar(2L);

        /*
         * Como a publicação falhou, o registro deve permanecer
         * disponível para uma futura tentativa do job.
         */
        assertEquals(
                StatusEventoOutbox.PENDENTE,
                eventoOutbox.getStatus()
        );

        assertNull(
                eventoOutbox.getPublicadoEm()
        );

        assertEquals(
                1,
                eventoOutbox.getQuantidadeTentativas()
        );

        assertNotNull(
                eventoOutbox.getUltimoErro()
        );

        assertTrue(
                eventoOutbox
                        .getUltimoErro()
                        .contains("Kafka indisponível")
        );

        /*
         * Confirma que o producer realmente recebeu
         * uma tentativa de publicação.
         */
        verify(
                pedidoProducer
        ).publicar(evento);

        /*
         * Confirma que a tentativa malsucedida também foi
         * registrada na telemetria da aplicação.
         */
        verify(
                outboxMetrics
        ).registrarFalhaPublicacao();

        /*
         * Sem confirmação do Kafka, não existe uma publicação
         * concluída cujo tempo deva ser registrado no Timer.
         */
        verify(
                outboxMetrics,
                never()
        ).registrarTempoAtePublicacao(
                any(Duration.class)
        );


    }

    /**
     * Verifica se um evento que não está mais pendente
     * é ignorado e não é publicado novamente.
     */
    @Test
    void deveIgnorarEventoQueJaFoiPublicado() {

        UUID eventoId = UUID.randomUUID();

        EventoOutbox eventoOutbox = new EventoOutbox(
                eventoId,
                PedidoCriadoEvent.class.getSimpleName(),
                "pedidos-criados",
                "30",
                "{}"
        );

        /*
         * Simula que o evento já foi publicado anteriormente.
         */
        eventoOutbox.marcarComoPublicado();

        /*
         * Simula a localização do registro no banco.
         */
        when(
                eventoOutboxRepository.findById(3L)
        ).thenReturn(
                Optional.of(eventoOutbox)
        );

        publicadorEventoOutboxService.publicar(3L);

        /*
         * O status deve continuar PUBLICADO.
         */
        assertEquals(
                StatusEventoOutbox.PUBLICADO,
                eventoOutbox.getStatus()
        );

        /*
         * Como o evento não estava pendente, o JSON não deve
         * ser convertido e o producer não deve ser chamado.
         */
        verifyNoInteractions(objectMapper);
        verifyNoInteractions(pedidoProducer);

        /*
         * Um evento ignorado não representa uma nova falha
         * de publicação e não deve alterar o Counter.
         */
        verifyNoInteractions(outboxMetrics);
    }
}
