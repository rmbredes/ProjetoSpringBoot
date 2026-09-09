package br.com.exemplo.projetospringboot.repository;


import br.com.exemplo.projetospringboot.entity.Pedido;
import br.com.exemplo.projetospringboot.entity.StatusLogisticaPedido;
import br.com.exemplo.projetospringboot.entity.StatusPagamentoPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Fornece as operações de persistência e as consultas específicas de pedidos.
 */
public interface PedidoRepository
        extends JpaRepository<Pedido, Long> {

    /**
     * Localiza todos os pedidos pertencentes ao cliente informado.
     *
     * @param clienteId identificador do cliente
     * @return pedidos associados ao cliente
     */
    List<Pedido> findByClienteId(
            Long clienteId
    );

    /**
     * Localiza pagamentos aprovados cuja logística ainda está pendente.
     *
     * Exigimos eventoPagamentoId preenchido para ignorar registros
     * históricos criados antes desse novo campo.
     */
    List<Pedido>
    findByStatusPagamentoAndStatusLogisticaAndPagamentoEventoIdIsNotNullOrderByDataCriacaoAsc(
            StatusPagamentoPedido statusPagamento,
            StatusLogisticaPedido statusLogistica
    );
}
