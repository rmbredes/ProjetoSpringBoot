package br.com.exemplo.projetospringboot.service;

import br.com.exemplo.projetospringboot.entity.EventoOutbox;
import br.com.exemplo.projetospringboot.enums.StatusEventoOutbox;
import br.com.exemplo.projetospringboot.event.PedidoCriadoEvent;
import br.com.exemplo.projetospringboot.messaging.producer.PedidoProducer;
import br.com.exemplo.projetospringboot.repository.EventoOutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
     * Cria o serviço testado e injeta automaticamente os mocks.
     */
    @InjectMocks
    private PublicadorEventoOutboxService publicadorEventoOutboxService;

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
         * Simula uma confirmação bem-sucedida do Kafka.
         */
        when(
                pedidoProducer.publicar(evento)
        ).thenReturn(
                CompletableFuture.completedFuture(null)
        );

        publicadorEventoOutboxService.publicar(1L);

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
    }
}