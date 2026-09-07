package br.com.exemplo.projetospringboot.service;

import br.com.exemplo.projetospringboot.dto.GerarRelatorioPedidoMensagem;
import br.com.exemplo.projetospringboot.dto.RelatorioPedidoDTO;
import br.com.exemplo.projetospringboot.entity.Pedido;
import br.com.exemplo.projetospringboot.entity.RelatorioPedido;
import br.com.exemplo.projetospringboot.enums.StatusRelatorioPedido;
import br.com.exemplo.projetospringboot.messaging.producer.RelatorioPedidoSqsProducer;
import br.com.exemplo.projetospringboot.repository.PedidoRepository;
import br.com.exemplo.projetospringboot.repository.RelatorioPedidoRepository;
import br.com.exemplo.projetospringboot.storage.S3StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do serviço que solicita relatórios.
 *
 * <p>Banco e SQS são simulados. O objetivo é verificar somente
 * a sequência coordenada pelo serviço.</p>
 */
@ExtendWith(MockitoExtension.class)
class RelatorioPedidoServiceTest {

    /** Simula o acesso aos pedidos. */
    @Mock
    private PedidoRepository pedidoRepository;

    /** Simula a persistência dos relatórios. */
    @Mock
    private RelatorioPedidoRepository relatorioPedidoRepository;

    /** Simula o producer sem acessar a AWS. */
    @Mock
    private RelatorioPedidoSqsProducer producer;

    /** Simula a geração de URLs temporárias do S3. */
    @Mock
    private S3StorageService storageService;

    /** Serviço submetido ao teste. */
    private RelatorioPedidoService service;

    /** Cria o serviço antes de cada teste. */
    @BeforeEach
    void configurarService() {
        service = new RelatorioPedidoService(
                pedidoRepository,
                relatorioPedidoRepository,
                producer,
                storageService
        );
    }

    /**
     * Confirma o fluxo PENDENTE → mensagem enviada → ENFILEIRADO.
     */
    @Test
    void deveSolicitarRelatorioEEnfileirarMensagem() {
        Pedido pedido = new Pedido();
        pedido.setId(3L);
        pedido.setDataCriacao(LocalDateTime.now());

        when(pedidoRepository.findById(3L))
                .thenReturn(Optional.of(pedido));

        when(
                relatorioPedidoRepository
                        .findFirstByPedidoIdOrderByCriadoEmDesc(3L)
        ).thenReturn(Optional.empty());

        /*
         * Em um banco real, o JPA preencheria o ID após o primeiro save.
         * Como o repositório é simulado, fazemos isso manualmente no teste.
         */
        when(relatorioPedidoRepository.save(any(RelatorioPedido.class)))
                .thenAnswer(invocacao -> {
                    RelatorioPedido relatorio = invocacao.getArgument(0);

                    if (relatorio.getId() == null) {
                        ReflectionTestUtils.setField(
                                relatorio,
                                "id",
                                15L
                        );
                    }

                    return relatorio;
                });

        when(producer.enviar(any(GerarRelatorioPedidoMensagem.class)))
                .thenReturn("mensagem-123");

        RelatorioPedidoDTO resultado = service.solicitar(3L);

        ArgumentCaptor<GerarRelatorioPedidoMensagem> mensagemCaptor =
                ArgumentCaptor.forClass(
                        GerarRelatorioPedidoMensagem.class
                );

        verify(producer).enviar(mensagemCaptor.capture());

        GerarRelatorioPedidoMensagem mensagem =
                mensagemCaptor.getValue();

        assertEquals(15L, mensagem.relatorioId());
        assertEquals(3L, mensagem.pedidoId());
        assertNotNull(mensagem.solicitacaoId());
        assertNotNull(mensagem.solicitadoEm());

        assertEquals(15L, resultado.id());
        assertEquals(3L, resultado.pedidoId());
        assertEquals(
                StatusRelatorioPedido.ENFILEIRADO,
                resultado.status()
        );

        /*
         * Primeiro save: cria PENDENTE.
         * Segundo save: persiste ENFILEIRADO.
         */
        verify(relatorioPedidoRepository, times(2))
                .save(any(RelatorioPedido.class));
    }

    /**
     * Confirma que uma nova chamada reutiliza o relatório existente
     * sem inserir outro registro e sem publicar outra mensagem.
     */
    @Test
    void deveReutilizarRelatorioExistenteDoMesmoPedido() {
        Pedido pedido = new Pedido();
        pedido.setId(803L);
        pedido.setDataCriacao(LocalDateTime.now());

        RelatorioPedido relatorioExistente =
                new RelatorioPedido(
                        java.util.UUID.randomUUID(),
                        pedido
                );

        ReflectionTestUtils.setField(
                relatorioExistente,
                "id",
                9L
        );

        relatorioExistente.marcarComoEnfileirado();
        relatorioExistente.marcarComoDisponivel(
                "pedidos/803/relatorios/relatorio.pdf",
                "relatorio-pedido-803.pdf",
                1414L,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                        + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "etag-relatorio"
        );

        when(pedidoRepository.findById(803L))
                .thenReturn(Optional.of(pedido));

        when(
                relatorioPedidoRepository
                        .findFirstByPedidoIdOrderByCriadoEmDesc(803L)
        ).thenReturn(Optional.of(relatorioExistente));

        RelatorioPedidoDTO resultado = service.solicitar(803L);

        assertEquals(9L, resultado.id());
        assertEquals(
                StatusRelatorioPedido.DISPONIVEL,
                resultado.status()
        );

        verify(producer, never())
                .enviar(any(GerarRelatorioPedidoMensagem.class));

        verify(relatorioPedidoRepository, never())
                .save(any(RelatorioPedido.class));
    }
}
