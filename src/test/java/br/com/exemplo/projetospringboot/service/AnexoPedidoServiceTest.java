package br.com.exemplo.projetospringboot.service;

import br.com.exemplo.projetospringboot.dto.AnexoPedidoDTO;
import br.com.exemplo.projetospringboot.entity.AnexoPedido;
import br.com.exemplo.projetospringboot.entity.Pedido;
import br.com.exemplo.projetospringboot.enums.StatusAnexoPedido;
import br.com.exemplo.projetospringboot.exception.ArmazenamentoAnexoException;
import br.com.exemplo.projetospringboot.exception.ArquivoAnexoInvalidoException;
import br.com.exemplo.projetospringboot.repository.AnexoPedidoRepository;
import br.com.exemplo.projetospringboot.repository.PedidoRepository;
import br.com.exemplo.projetospringboot.storage.S3StorageService;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Testa as regras de negócio do upload de anexos.
 *
 * <p>PedidoRepository, AnexoPedidoRepository e S3StorageService
 * são substituídos por mocks. Portanto, estes testes não acessam
 * banco, rede ou AWS.</p>
 */
@ExtendWith(MockitoExtension.class)
class AnexoPedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private AnexoPedidoRepository anexoPedidoRepository;

    @Mock
    private S3StorageService storageService;

    private AnexoPedidoService service;

    /**
     * Cria o serviço antes de cada teste.
     *
     * <p>ObservationRegistry.NOOP permite executar as observações
     * sem precisar configurar exportadores de métricas ou tracing.</p>
     */
    @BeforeEach
    void configurarTeste() {
        service = new AnexoPedidoService(
                pedidoRepository,
                anexoPedidoRepository,
                storageService,
                ObservationRegistry.NOOP
        );
    }

    /**
     * Confirma o fluxo bem-sucedido:
     *
     * <pre>
     * validar
     * → localizar pedido
     * → salvar PENDENTE
     * → enviar ao S3
     * → salvar DISPONIVEL
     * → devolver DTO
     * </pre>
     */
    @Test
    void deveEnviarAnexoERegistrarComoDisponivel() {
        Pedido pedido = criarPedido(10L);

        byte[] conteudo =
                "conteúdo do arquivo".getBytes();

        MockMultipartFile arquivo =
                new MockMultipartFile(
                        "arquivo",
                        "documentos/nota-fiscal.pdf",
                        "application/pdf",
                        conteudo
                );

        when(pedidoRepository.findById(10L))
                .thenReturn(Optional.of(pedido));

        /*
         * Registra o status existente em cada chamada ao save.
         *
         * Como o mesmo objeto é alterado durante o fluxo, salvamos
         * o valor do enum no instante de cada chamada.
         */
        List<StatusAnexoPedido> estadosSalvos =
                new ArrayList<>();

        when(anexoPedidoRepository.save(
                any(AnexoPedido.class)
        )).thenAnswer(invocacao -> {
            AnexoPedido anexo =
                    invocacao.getArgument(0);

            estadosSalvos.add(anexo.getStatus());

            return anexo;
        });

        when(storageService.upload(
                anyString(),
                any(InputStream.class),
                anyLong(),
                anyString()
        )).thenReturn("\"etag-real-simulado\"");

        AnexoPedidoDTO resultado =
                service.enviar(10L, arquivo);

        /*
         * Captura a key gerada pelo serviço.
         */
        ArgumentCaptor<String> keyCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(storageService).upload(
                keyCaptor.capture(),
                any(InputStream.class),
                eq((long) conteudo.length),
                eq("application/pdf")
        );

        assertThat(keyCaptor.getValue())
                .startsWith("pedidos/10/anexos/");

        /*
         * A key termina em um UUID e não contém o nome original.
         */
        assertThat(keyCaptor.getValue())
                .doesNotContain("nota-fiscal.pdf");

        assertThat(estadosSalvos)
                .containsExactly(
                        StatusAnexoPedido.PENDENTE,
                        StatusAnexoPedido.DISPONIVEL
                );

        assertThat(resultado.pedidoId())
                .isEqualTo(10L);

        /*
         * O caminho recebido foi removido e somente o nome
         * final permanece.
         */
        assertThat(resultado.nomeOriginal())
                .isEqualTo("nota-fiscal.pdf");

        assertThat(resultado.contentType())
                .isEqualTo("application/pdf");

        assertThat(resultado.tamanhoBytes())
                .isEqualTo(conteudo.length);

        assertThat(resultado.checksumSha256())
                .hasSize(64)
                .matches("[0-9a-f]{64}");

        assertThat(resultado.status())
                .isEqualTo(StatusAnexoPedido.DISPONIVEL);
    }

    /**
     * Confirma que um tipo não permitido é rejeitado
     * antes de consultar o banco ou acessar o S3.
     */
    @Test
    void naoDeveAceitarTipoDeArquivoNaoPermitido() {
        MockMultipartFile arquivo =
                new MockMultipartFile(
                        "arquivo",
                        "programa.exe",
                        "application/octet-stream",
                        "conteúdo".getBytes()
                );

        assertThatThrownBy(() ->
                service.enviar(10L, arquivo)
        )
                .isInstanceOf(
                        ArquivoAnexoInvalidoException.class
                )
                .hasMessageContaining(
                        "Tipo de arquivo não permitido"
                );

        verifyNoInteractions(
                pedidoRepository,
                anexoPedidoRepository,
                storageService
        );
    }

    /**
     * Confirma que nenhum upload ocorre quando o pedido não existe.
     */
    @Test
    void naoDeveEnviarAnexoParaPedidoInexistente() {
        MockMultipartFile arquivo =
                new MockMultipartFile(
                        "arquivo",
                        "arquivo.txt",
                        "text/plain",
                        "conteúdo".getBytes()
                );

        when(pedidoRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.enviar(999L, arquivo)
        )
                .isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessage("Pedido não encontrado");

        verify(storageService, never()).upload(
                anyString(),
                any(InputStream.class),
                anyLong(),
                anyString()
        );

        verifyNoInteractions(anexoPedidoRepository);
    }

    /**
     * Confirma que uma falha do S3 é registrada no banco
     * e transformada em uma exceção da aplicação.
     */
    @Test
    void deveRegistrarFalhaQuandoUploadNoS3Falhar() {
        Pedido pedido = criarPedido(20L);

        MockMultipartFile arquivo =
                new MockMultipartFile(
                        "arquivo",
                        "arquivo.txt",
                        "text/plain",
                        "conteúdo".getBytes()
                );

        when(pedidoRepository.findById(20L))
                .thenReturn(Optional.of(pedido));

        List<StatusAnexoPedido> estadosSalvos =
                new ArrayList<>();

        when(anexoPedidoRepository.save(
                any(AnexoPedido.class)
        )).thenAnswer(invocacao -> {
            AnexoPedido anexo =
                    invocacao.getArgument(0);

            estadosSalvos.add(anexo.getStatus());

            return anexo;
        });

        when(storageService.upload(
                anyString(),
                any(InputStream.class),
                anyLong(),
                eq("text/plain")
        )).thenThrow(
                new RuntimeException(
                        "Falha simulada do S3"
                )
        );

        assertThatThrownBy(() ->
                service.enviar(20L, arquivo)
        )
                .isInstanceOf(
                        ArmazenamentoAnexoException.class
                )
                .hasMessage(
                        "Não foi possível armazenar o anexo"
                )
                .hasRootCauseMessage(
                        "Falha simulada do S3"
                );

        assertThat(estadosSalvos)
                .containsExactly(
                        StatusAnexoPedido.PENDENTE,
                        StatusAnexoPedido.FALHA
                );
    }

    /**
     * Cria um pedido válido com identificador conhecido.
     */
    private Pedido criarPedido(Long id) {
        return new Pedido(
                id,
                new BigDecimal("100.00"),
                LocalDateTime.now(),
                null
        );
    }
}