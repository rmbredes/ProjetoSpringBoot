package br.com.exemplo.projetospringboot.service;

import br.com.exemplo.projetospringboot.dto.GerarRelatorioPedidoMensagem;
import br.com.exemplo.projetospringboot.dto.RelatorioPedidoDisponivelEvento;
import br.com.exemplo.projetospringboot.entity.Cliente;
import br.com.exemplo.projetospringboot.entity.Pedido;
import br.com.exemplo.projetospringboot.entity.RelatorioPedido;
import br.com.exemplo.projetospringboot.enums.StatusRelatorioPedido;
import br.com.exemplo.projetospringboot.messaging.publisher.RelatorioPedidoSnsPublisher;
import br.com.exemplo.projetospringboot.report.GeradorRelatorioPedidoPdf;
import br.com.exemplo.projetospringboot.repository.RelatorioPedidoRepository;
import br.com.exemplo.projetospringboot.storage.S3StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Testa a coordenação entre PDF, S3 e banco de dados. */
@ExtendWith(MockitoExtension.class)
class ProcessadorRelatorioPedidoServiceTest {

    @Mock
    private RelatorioPedidoRepository repository;

    @Mock
    private GeradorRelatorioPedidoPdf geradorPdf;

    @Mock
    private S3StorageService storageService;

    /** Simula a publicação do evento sem acessar o tópico real. */
    @Mock
    private RelatorioPedidoSnsPublisher snsPublisher;

    private ProcessadorRelatorioPedidoService service;

    /** Prepara o serviço usando somente dependências simuladas. */
    @BeforeEach
    void configurarService() {
        service = new ProcessadorRelatorioPedidoService(
                repository,
                geradorPdf,
                storageService,
                Optional.of(snsPublisher)
        );
    }

    /** Confirma que um PDF concluído é registrado como DISPONIVEL. */
    @Test
    void deveGerarEnviarEConcluirRelatorio() {
        UUID solicitacaoId = UUID.randomUUID();

        Cliente cliente = new Cliente(
                7L,
                "Ricardo",
                "ricardo@exemplo.com",
                true
        );

        Pedido pedido = new Pedido(
                799L,
                new BigDecimal("149.90"),
                LocalDateTime.now(),
                cliente
        );

        RelatorioPedido relatorio = new RelatorioPedido(
                solicitacaoId,
                pedido
        );

        ReflectionTestUtils.setField(relatorio, "id", 1L);
        relatorio.marcarComoEnfileirado();

        GerarRelatorioPedidoMensagem mensagem =
                new GerarRelatorioPedidoMensagem(
                        solicitacaoId,
                        1L,
                        799L,
                        Instant.now()
                );

        byte[] pdf = "%PDF-conteudo-simulado".getBytes();

        when(repository.findBySolicitacaoId(solicitacaoId))
                .thenReturn(Optional.of(relatorio));

        when(geradorPdf.gerar(relatorio))
                .thenReturn(pdf);

        when(storageService.upload(
                eq("pedidos/799/relatorios/"
                        + solicitacaoId
                        + ".pdf"),
                any(InputStream.class),
                eq((long) pdf.length),
                eq("application/pdf")
        )).thenReturn("etag-relatorio");

        service.processar(mensagem);

        assertEquals(
                StatusRelatorioPedido.DISPONIVEL,
                relatorio.getStatus()
        );
        assertEquals(
                "relatorio-pedido-799.pdf",
                relatorio.getNomeArquivo()
        );
        assertEquals((long) pdf.length, relatorio.getTamanhoBytes());
        assertEquals(64, relatorio.getChecksumSha256().length());
        assertNotNull(relatorio.getConcluidoEm());

        verify(repository).save(relatorio);

        ArgumentCaptor<RelatorioPedidoDisponivelEvento> eventoCaptor =
                ArgumentCaptor.forClass(
                        RelatorioPedidoDisponivelEvento.class
                );

        verify(snsPublisher).publicar(eventoCaptor.capture());

        RelatorioPedidoDisponivelEvento evento =
                eventoCaptor.getValue();

        assertNotNull(evento.eventoId());
        assertEquals(
                "RELATORIO_PEDIDO_DISPONIVEL",
                evento.tipo()
        );
        assertEquals(1L, evento.relatorioId());
        assertEquals(799L, evento.pedidoId());
        assertEquals(solicitacaoId, evento.solicitacaoId());
        assertEquals(relatorio.getObjectKey(), evento.objectKey());
        assertNotNull(evento.ocorridoEm());
    }
}
