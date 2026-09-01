package br.com.exemplo.projetospringboot.messaging.consumer;

import br.com.exemplo.projetospringboot.event.PedidoCriadoEvent;
import br.com.exemplo.projetospringboot.observability.logging.EventoLogContext;
import br.com.exemplo.projetospringboot.observability.metrics.KafkaProcessamentoMetrics;
import br.com.exemplo.projetospringboot.service.EstoqueService;
import io.micrometer.core.instrument.Timer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Verifica a correlação dos logs produzidos
 * durante o consumo dos eventos de estoque.
 */
@ExtendWith(MockitoExtension.class)
class EstoquePedidoConsumerTest {

    /**
     * Simula o serviço que contém as regras de estoque.
     */
    @Mock
    private EstoqueService estoqueService;

    /**
     * Simula o componente responsável pela métrica.
     */
    @Mock
    private KafkaProcessamentoMetrics kafkaProcessamentoMetrics;

    /**
     * Representa o instante inicial da medição.
     */
    @Mock
    private Timer.Sample amostra;

    /**
     * Cria o consumer e injeta o serviço simulado.
     */
    @InjectMocks
    private EstoquePedidoConsumer estoquePedidoConsumer;

    /**
     * Faz cada teste utilizar uma amostra controlada.
     */
    @BeforeEach
    void prepararMetrica() {
        when(
                kafkaProcessamentoMetrics.iniciarMedicao()
        ).thenReturn(
                amostra
        );
    }

    /**
     * Garante que nenhum teste deixe informações
     * no MDC da thread utilizada pelo JUnit.
     */
    @AfterEach
    void limparMdc() {
        MDC.clear();
    }

    /**
     * Verifica se o eventoId está disponível no MDC
     * durante o processamento realizado pelo serviço.
     */
    @Test
    void deveDisponibilizarEventoIdDuranteProcessamento() {
        UUID eventoId = UUID.randomUUID();

        PedidoCriadoEvent evento = new PedidoCriadoEvent(
                eventoId,
                10L,
                1L,
                new BigDecimal("150.00"),
                Instant.now()
        );

        /*
         * Representa uma mensagem que teria sido
         * recebida do tópico pedidos-criados.
         */
        ConsumerRecord<String, PedidoCriadoEvent> registro =
                new ConsumerRecord<>(
                        "pedidos-criados",
                        0,
                        15L,
                        "10",
                        evento
                );

        /*
         * Verifica o MDC exatamente quando o consumer
         * entrega o evento para a camada de serviço.
         */
        when(
                estoqueService.processar(evento)
        ).thenAnswer(invocacao -> {
            assertEquals(
                    eventoId.toString(),
                    MDC.get(
                            EventoLogContext.CHAVE_EVENTO_ID
                    )
            );

            return true;
        });

        estoquePedidoConsumer.processar(registro);

        /*
         * Confirma que o serviço recebeu o evento correto.
         */
        verify(
                estoqueService
        ).processar(evento);

        /*
         * O caminho normal deve ser registrado como sucesso.
         */
        verify(
                kafkaProcessamentoMetrics
        ).finalizarMedicao(
                amostra,
                "estoque",
                true
        );

        /*
         * Ao terminar o consumo, o eventoId precisa
         * ter sido removido da thread.
         */
        assertNull(
                MDC.get(
                        EventoLogContext.CHAVE_EVENTO_ID
                )
        );
    }

    /**
     * Verifica se o MDC também é limpo quando
     * o processamento termina com uma exceção.
     */
    @Test
    void deveLimparMdcQuandoProcessamentoFalhar() {
        UUID eventoId = UUID.randomUUID();

        PedidoCriadoEvent evento = new PedidoCriadoEvent(
                eventoId,
                11L,
                1L,
                new BigDecimal("999.99"),
                Instant.now()
        );

        ConsumerRecord<String, PedidoCriadoEvent> registro =
                new ConsumerRecord<>(
                        "pedidos-criados",
                        0,
                        16L,
                        "11",
                        evento
                );

        /*
         * O valor 999.99 provoca a exceção simulada
         * dentro do próprio consumer.
         */
        assertThrows(
                IllegalStateException.class,
                () -> estoquePedidoConsumer.processar(
                        registro
                )
        );

        /*
         * Mesmo com a exceção, o try-with-resources
         * deve fechar e limpar o contexto.
         */
        assertNull(
                MDC.get(
                        EventoLogContext.CHAVE_EVENTO_ID
                )
        );

        /*
         * A falha ocorre antes da chamada ao serviço.
         */
        verifyNoInteractions(
                estoqueService
        );

        /*
         * Mesmo lançando exceção, o finally deve registrar
         * a duração com resultado de falha.
         */
        verify(
                kafkaProcessamentoMetrics
        ).finalizarMedicao(
                amostra,
                "estoque",
                false
        );
    }
}
