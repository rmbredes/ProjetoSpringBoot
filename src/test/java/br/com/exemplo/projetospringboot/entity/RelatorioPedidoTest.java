package br.com.exemplo.projetospringboot.entity;

import br.com.exemplo.projetospringboot.enums.StatusRelatorioPedido;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Valida as transições de estado da entidade de relatório
 * sem acessar banco, SQS ou S3.
 */
class RelatorioPedidoTest {

    /**
     * Uma nova solicitação deve começar pendente e ainda não pode
     * possuir informações de um arquivo que não foi gerado.
     */
    @Test
    void deveCriarRelatorioPendente() {
        Pedido pedido = new Pedido();
        UUID solicitacaoId = UUID.randomUUID();

        RelatorioPedido relatorio = new RelatorioPedido(
                solicitacaoId,
                pedido
        );

        assertEquals(
                solicitacaoId,
                relatorio.getSolicitacaoId()
        );
        assertEquals(
                StatusRelatorioPedido.PENDENTE,
                relatorio.getStatus()
        );
        assertEquals(0, relatorio.getQuantidadeTentativas());
        assertNotNull(relatorio.getCriadoEm());
        assertNotNull(relatorio.getAtualizadoEm());
        assertNull(relatorio.getObjectKey());
        assertNull(relatorio.getConcluidoEm());
    }

    /**
     * Depois da confirmação do SQS, a solicitação passa a aguardar
     * a execução do consumer.
     */
    @Test
    void deveMarcarRelatorioComoEnfileirado() {
        RelatorioPedido relatorio = novoRelatorio();

        relatorio.marcarComoEnfileirado();

        assertEquals(
                StatusRelatorioPedido.ENFILEIRADO,
                relatorio.getStatus()
        );
        assertNull(relatorio.getUltimoErro());
    }

    /**
     * A conclusão reúne os metadados que permitirão validar
     * e disponibilizar o PDF armazenado no S3.
     */
    @Test
    void deveMarcarRelatorioComoDisponivel() {
        RelatorioPedido relatorio = novoRelatorio();
        relatorio.marcarComoEnfileirado();

        relatorio.marcarComoDisponivel(
                "pedidos/1/relatorios/arquivo.pdf",
                "relatorio-pedido-1.pdf",
                2048L,
                "a".repeat(64),
                "etag-exemplo"
        );

        assertEquals(
                StatusRelatorioPedido.DISPONIVEL,
                relatorio.getStatus()
        );
        assertEquals(
                "application/pdf",
                relatorio.getContentType()
        );
        assertEquals(2048L, relatorio.getTamanhoBytes());
        assertNotNull(relatorio.getConcluidoEm());
    }

    /** Um relatório concluído não poderá voltar para o estado de falha. */
    @Test
    void naoDeveMarcarRelatorioDisponivelComoFalha() {
        RelatorioPedido relatorio = novoRelatorio();
        relatorio.marcarComoEnfileirado();
        relatorio.marcarComoDisponivel(
                "pedidos/1/relatorios/arquivo.pdf",
                "relatorio-pedido-1.pdf",
                2048L,
                "a".repeat(64),
                "etag-exemplo"
        );

        assertThrows(
                IllegalStateException.class,
                () -> relatorio.marcarComoFalha(
                        "falha tardia"
                )
        );
    }

    /** Cria uma entidade válida para os diferentes cenários do teste. */
    private RelatorioPedido novoRelatorio() {
        return new RelatorioPedido(
                UUID.randomUUID(),
                new Pedido()
        );
    }
}
