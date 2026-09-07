package br.com.exemplo.projetospringboot.repository;

import br.com.exemplo.projetospringboot.entity.RelatorioPedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório responsável por persistir e consultar
 * as solicitações de relatório de pedido.
 */
public interface RelatorioPedidoRepository
        extends JpaRepository<RelatorioPedido, Long> {

    /**
     * Localiza uma solicitação por seu UUID público.
     *
     * @param solicitacaoId identificador transportado pela mensagem SQS
     * @return relatório correspondente, quando existir
     */
    @EntityGraph(attributePaths = {"pedido", "pedido.cliente"})
    Optional<RelatorioPedido> findBySolicitacaoId(
            UUID solicitacaoId
    );

    /**
     * Garante que a consulta respeite simultaneamente o relatório
     * solicitado e o pedido informado na URL.
     *
     * @param id identificador interno do relatório
     * @param pedidoId identificador do pedido proprietário
     * @return relatório encontrado, quando ambos coincidirem
     */
    Optional<RelatorioPedido> findByIdAndPedidoId(
            Long id,
            Long pedidoId
    );

    /**
     * Lista os relatórios de um pedido, apresentando primeiro
     * as solicitações mais recentes.
     *
     * @param pedidoId identificador do pedido
     * @return relatórios associados ao pedido
     */
    List<RelatorioPedido> findAllByPedidoIdOrderByCriadoEmDesc(
            Long pedidoId
    );

    /**
     * Localiza somente o relatório mais recente de um pedido.
     *
     * <p>Essa consulta permite reutilizar uma solicitação existente,
     * evitando novas mensagens e novos arquivos para o mesmo pedido.</p>
     *
     * @param pedidoId identificador do pedido
     * @return relatório mais recente, quando existir
     */
    @EntityGraph(attributePaths = "pedido")
    Optional<RelatorioPedido> findFirstByPedidoIdOrderByCriadoEmDesc(
            Long pedidoId
    );
}
