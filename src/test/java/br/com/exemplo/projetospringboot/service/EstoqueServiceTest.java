package br.com.exemplo.projetospringboot.service;

import br.com.exemplo.projetospringboot.entity.EventoProcessado;
import br.com.exemplo.projetospringboot.event.PedidoCriadoEvent;
import br.com.exemplo.projetospringboot.repository.EventoProcessadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários das regras de processamento
 * e idempotência do EstoqueService.
 *
 * O banco e o Kafka não são utilizados nestes testes.
 * O repository é substituído por um mock.
 */
@ExtendWith(MockitoExtension.class)
class EstoqueServiceTest {

    /*
     * Mock usado para controlar as consultas e verificar
     * as gravações de eventos processados.
     */
    @Mock
    private EventoProcessadoRepository eventoProcessadoRepository;

    /*
     * Serviço testado, construído pelo Mockito com
     * o repository simulado.
     */
    @InjectMocks
    private EstoqueService estoqueService;

    /*
     * Evento reutilizado nos testes.
     */
    private PedidoCriadoEvent evento;

    /**
     * Prepara um evento conhecido antes de cada teste.
     */
    @BeforeEach
    void prepararDados() {
        evento = new PedidoCriadoEvent(
                UUID.fromString(
                        "a3e7cdbe-56d9-4269-8ae0-044c7e5f7d7a"
                ),
                28L,
                1L,
                new BigDecimal("888.88"),
                Instant.parse("2026-08-28T11:32:30Z")
        );
    }

    /**
     * Verifica que um evento ainda não processado executa
     * o efeito e gera um registro de idempotência.
     */
    @Test
    void deveProcessarEventoAindaNaoProcessado() {
        /*
         * Simula que nenhuma ocorrência foi encontrada.
         */
        when(
                eventoProcessadoRepository
                        .contarPorEventoEConsumer(
                                evento.eventoId(),
                                "estoque-service-group"
                        )
        ).thenReturn(0L);

        boolean processadoAgora =
                estoqueService.processar(evento);

        assertTrue(processadoAgora);

        /*
         * Captura a entidade entregue ao repository para
         * verificar os dados que seriam persistidos.
         */
        ArgumentCaptor<EventoProcessado> captor =
                ArgumentCaptor.forClass(
                        EventoProcessado.class
                );

        verify(
                eventoProcessadoRepository
        ).save(
                captor.capture()
        );

        EventoProcessado registroSalvo =
                captor.getValue();

        assertEquals(
                evento.eventoId(),
                registroSalvo.getEventoId()
        );

        assertEquals(
                "estoque-service-group",
                registroSalvo.getNomeConsumer()
        );
    }

    /**
     * Verifica que um evento já processado é ignorado
     * e não gera uma segunda gravação.
     */
    @Test
    void deveIgnorarEventoJaProcessado() {
        /*
         * Simula que o evento já está registrado.
         */
        when(
                eventoProcessadoRepository
                        .contarPorEventoEConsumer(
                                evento.eventoId(),
                                "estoque-service-group"
                        )
        ).thenReturn(1L);

        boolean processadoAgora =
                estoqueService.processar(evento);

        assertFalse(processadoAgora);

        /*
         * Confirma que nenhuma nova entidade foi salva.
         */
        verify(
                eventoProcessadoRepository,
                never()
        ).save(
                any(EventoProcessado.class)
        );
    }
}