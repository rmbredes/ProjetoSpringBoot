package br.com.exemplo.projetospringboot.service;

import br.com.exemplo.projetospringboot.entity.Pedido;
import br.com.exemplo.projetospringboot.entity.StatusLogisticaPedido;
import br.com.exemplo.projetospringboot.entity.StatusPagamentoPedido;
import br.com.exemplo.projetospringboot.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Controla o estado persistente da integração com a logística.
 */
@Service
public class IntegracaoLogisticaService {

    private final PedidoRepository pedidoRepository;

    public IntegracaoLogisticaService(
            PedidoRepository pedidoRepository
    ) {
        this.pedidoRepository = pedidoRepository;
    }

    /**
     * Busca os pedidos que podem ser enviados à logística.
     *
     * A transação termina antes da chamada HTTP.
     */
    @Transactional(readOnly = true)
    public List<Pedido> buscarPendentes() {
        return pedidoRepository
                .findByStatusPagamentoAndStatusLogisticaAndPagamentoEventoIdIsNotNullOrderByDataCriacaoAsc(
                        StatusPagamentoPedido.APROVADO,
                        StatusLogisticaPedido.PENDENTE
                );
    }

    /**
     * Confirma no monólito o resultado devolvido pela logística.
     */
    @Transactional
    public void confirmar(
            Long pedidoId
    ) {
        Pedido pedido = pedidoRepository
                .findById(pedidoId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Pedido não encontrado: " + pedidoId
                        )
                );

        pedido.confirmarLogistica();

        /*
         * O UPDATE será executado pelo dirty checking do JPA.
         */
    }
}