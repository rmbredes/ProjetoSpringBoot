package br.com.exemplo.projetospringboot.entity;

import br.com.exemplo.projetospringboot.enums.StatusAnexoPedido;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testa as regras internas da entidade AnexoPedido.
 *
 * <p>Este é um teste unitário puro:</p>
 *
 * <ul>
 *     <li>não inicia o Spring;</li>
 *     <li>não acessa banco;</li>
 *     <li>não acessa o S3;</li>
 *     <li>não utiliza mocks.</li>
 * </ul>
 */
class AnexoPedidoTest {

    /**
     * Hash fictício com exatamente 64 caracteres,
     * equivalente ao formato hexadecimal de um SHA-256.
     */
    private static final String CHECKSUM_SHA256 =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                    + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    /**
     * Confirma o estado inicial de um anexo recém-criado.
     */
    @Test
    void deveCriarAnexoComoPendente() {
        Pedido pedido = criarPedido();

        AnexoPedido anexo = criarAnexo(pedido);

        assertThat(anexo.getPedido())
                .isSameAs(pedido);

        assertThat(anexo.getStatus())
                .isEqualTo(StatusAnexoPedido.PENDENTE);

        assertThat(anexo.getETag())
                .isNull();

        assertThat(anexo.getUltimoErro())
                .isNull();

        assertThat(anexo.getCriadoEm())
                .isNotNull();

        assertThat(anexo.getAtualizadoEm())
                .isNotNull();
    }

    /**
     * Confirma a transição:
     *
     * <pre>
     * PENDENTE → DISPONIVEL
     * </pre>
     */
    @Test
    void deveMarcarAnexoComoDisponivel() {
        AnexoPedido anexo = criarAnexo(criarPedido());

        anexo.marcarComoDisponivel("\"etag-teste\"");

        assertThat(anexo.getStatus())
                .isEqualTo(StatusAnexoPedido.DISPONIVEL);

        assertThat(anexo.getETag())
                .isEqualTo("\"etag-teste\"");

        assertThat(anexo.getUltimoErro())
                .isNull();
    }

    /**
     * Confirma a transição:
     *
     * <pre>
     * PENDENTE → FALHA
     * </pre>
     */
    @Test
    void deveRegistrarFalhaNoUpload() {
        AnexoPedido anexo = criarAnexo(criarPedido());

        anexo.marcarComoFalha(
                "Falha simulada ao acessar o S3"
        );

        assertThat(anexo.getStatus())
                .isEqualTo(StatusAnexoPedido.FALHA);

        assertThat(anexo.getUltimoErro())
                .isEqualTo(
                        "Falha simulada ao acessar o S3"
                );

        assertThat(anexo.getETag())
                .isNull();
    }

    /**
     * Confirma o ciclo bem-sucedido completo:
     *
     * <pre>
     * PENDENTE → DISPONIVEL → EXCLUIDO
     * </pre>
     */
    @Test
    void deveMarcarAnexoDisponivelComoExcluido() {
        AnexoPedido anexo = criarAnexo(criarPedido());

        anexo.marcarComoDisponivel("\"etag-teste\"");
        anexo.marcarComoExcluido();

        assertThat(anexo.getStatus())
                .isEqualTo(StatusAnexoPedido.EXCLUIDO);
    }

    /**
     * Impede excluir um anexo cujo upload não foi confirmado.
     */
    @Test
    void naoDeveExcluirAnexoPendente() {
        AnexoPedido anexo = criarAnexo(criarPedido());

        assertThatThrownBy(anexo::marcarComoExcluido)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DISPONIVEL")
                .hasMessageContaining("PENDENTE");
    }

    /**
     * Impede criar metadados para um arquivo vazio.
     */
    @Test
    void naoDeveCriarAnexoComTamanhoZero() {
        Pedido pedido = criarPedido();

        assertThatThrownBy(() ->
                new AnexoPedido(
                        pedido,
                        "pedidos/1/anexos/arquivo",
                        "arquivo.txt",
                        "text/plain",
                        0L,
                        CHECKSUM_SHA256
                )
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tamanhoBytes");
    }

    /**
     * Cria um pedido simples usado pelos testes.
     */
    private Pedido criarPedido() {
        Cliente cliente = new Cliente(
                null,
                "Cliente de teste",
                "cliente@teste.local",
                true
        );

        return new Pedido(
                null,
                new BigDecimal("100.00"),
                LocalDateTime.now(),
                cliente
        );
    }

    /**
     * Cria um anexo válido no estado inicial PENDENTE.
     */
    private AnexoPedido criarAnexo(Pedido pedido) {
        return new AnexoPedido(
                pedido,
                "pedidos/1/anexos/arquivo-teste",
                "arquivo-teste.txt",
                "text/plain",
                100L,
                CHECKSUM_SHA256
        );
    }
}