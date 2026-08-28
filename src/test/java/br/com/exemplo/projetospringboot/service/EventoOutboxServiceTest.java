package br.com.exemplo.projetospringboot.service;

import br.com.exemplo.projetospringboot.entity.EventoOutbox;
import br.com.exemplo.projetospringboot.enums.StatusEventoOutbox;
import br.com.exemplo.projetospringboot.event.PedidoCriadoEvent;
import br.com.exemplo.projetospringboot.repository.EventoOutboxRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do serviço responsável por registrar
 * eventos na tabela de Outbox.
 */
@ExtendWith(MockitoExtension.class)
class EventoOutboxServiceTest {

    /**
     * Simula o repositório utilizado para salvar
     * os registros da Outbox.
     */
    @Mock
    private EventoOutboxRepository eventoOutboxRepository;

    /**
     * Simula o componente responsável por converter
     * os eventos Java para JSON.
     */
    @Mock
    private ObjectMapper objectMapper;

    /**
     * Cria o serviço testado e injeta automaticamente os mocks.
     */
    @InjectMocks
    private EventoOutboxService eventoOutboxService;

    /**
     * Verifica se um evento de pedido é convertido para JSON
     * e registrado na Outbox com o status PENDENTE.
     *
     * @throws JacksonException caso o mock da conversão JSON falhe
     */
    @Test
    void deveRegistrarPedidoCriadoNaOutbox()
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

        /*
         * Simula o JSON produzido pelo ObjectMapper.
         */
        when(
                objectMapper.writeValueAsString(evento)
        ).thenReturn(payload);

        /*
         * Executa o registro do evento na Outbox.
         *
         * Neste teste unitário não existe o proxy transacional
         * do Spring, portanto a propagação MANDATORY não é aplicada.
         */
        eventoOutboxService.registrarPedidoCriado(evento);

        /*
         * Captura a entidade entregue ao repositório.
         */
        ArgumentCaptor<EventoOutbox> eventoOutboxCaptor =
                ArgumentCaptor.forClass(EventoOutbox.class);

        verify(
                eventoOutboxRepository
        ).save(
                eventoOutboxCaptor.capture()
        );

        EventoOutbox eventoOutboxSalvo =
                eventoOutboxCaptor.getValue();

        /*
         * Confirma os dados utilizados para a futura
         * publicação no Kafka.
         */
        assertEquals(
                eventoId,
                eventoOutboxSalvo.getEventoId()
        );

        assertEquals(
                PedidoCriadoEvent.class.getSimpleName(),
                eventoOutboxSalvo.getTipoEvento()
        );

        assertEquals(
                "pedidos-criados",
                eventoOutboxSalvo.getTopico()
        );

        assertEquals(
                "10",
                eventoOutboxSalvo.getChaveMensagem()
        );

        assertEquals(
                payload,
                eventoOutboxSalvo.getPayload()
        );

        assertEquals(
                StatusEventoOutbox.PENDENTE,
                eventoOutboxSalvo.getStatus()
        );

        assertEquals(
                0,
                eventoOutboxSalvo.getQuantidadeTentativas()
        );

        assertNotNull(
                eventoOutboxSalvo.getCriadoEm()
        );

        assertNull(
                eventoOutboxSalvo.getPublicadoEm()
        );

        assertNull(
                eventoOutboxSalvo.getUltimoErro()
        );
    }
}