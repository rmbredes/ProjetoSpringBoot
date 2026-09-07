package br.com.exemplo.projetospringboot.service;

import br.com.exemplo.projetospringboot.dto.RelatorioPedidoDisponivelEvento;
import br.com.exemplo.projetospringboot.entity.AuditoriaRelatorio;
import br.com.exemplo.projetospringboot.repository.AuditoriaRelatorioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Testa as regras de persistência e idempotência da auditoria. */
@ExtendWith(MockitoExtension.class)
class AuditoriaRelatorioServiceTest {

    @Mock
    private AuditoriaRelatorioRepository repository;

    /** Confirma que todos os dados recebidos no evento são persistidos. */
    @Test
    void deveRegistrarEventoNovo() {
        UUID eventoId = UUID.randomUUID();
        UUID solicitacaoId = UUID.randomUUID();
        Instant ocorridoEm = Instant.parse("2026-09-04T19:21:30Z");

        RelatorioPedidoDisponivelEvento evento =
                new RelatorioPedidoDisponivelEvento(
                        eventoId,
                        "RELATORIO_PEDIDO_DISPONIVEL",
                        14L,
                        808L,
                        solicitacaoId,
                        "pedidos/808/relatorios/arquivo.pdf",
                        ocorridoEm
                );

        when(repository.existsByEventoId(eventoId))
                .thenReturn(false);

        AuditoriaRelatorioService service =
                new AuditoriaRelatorioService(repository);

        service.registrar(evento);

        ArgumentCaptor<AuditoriaRelatorio> captor =
                ArgumentCaptor.forClass(AuditoriaRelatorio.class);

        verify(repository).save(captor.capture());

        AuditoriaRelatorio salvo = captor.getValue();
        assertEquals(eventoId, salvo.getEventoId());
        assertEquals(14L, salvo.getRelatorioId());
        assertEquals(808L, salvo.getPedidoId());
        assertEquals(solicitacaoId, salvo.getSolicitacaoId());
        assertEquals(ocorridoEm, salvo.getOcorridoEm());
    }

    /**
     * Simula uma nova entrega do mesmo evento pelo SQS.
     * O eventoId já existente impede a criação de uma linha duplicada.
     */
    @Test
    void naoDeveDuplicarEventoJaRegistrado() {
        UUID eventoId = UUID.randomUUID();

        RelatorioPedidoDisponivelEvento evento =
                new RelatorioPedidoDisponivelEvento(
                        eventoId,
                        "RELATORIO_PEDIDO_DISPONIVEL",
                        14L,
                        808L,
                        UUID.randomUUID(),
                        "pedidos/808/relatorios/arquivo.pdf",
                        Instant.now()
                );

        when(repository.existsByEventoId(eventoId))
                .thenReturn(true);

        AuditoriaRelatorioService service =
                new AuditoriaRelatorioService(repository);

        service.registrar(evento);

        verify(repository, never()).save(
                org.mockito.ArgumentMatchers.any()
        );
    }
}
