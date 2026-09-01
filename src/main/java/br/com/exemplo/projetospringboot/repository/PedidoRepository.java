package br.com.exemplo.projetospringboot.repository;


import br.com.exemplo.projetospringboot.entity.Pedido;
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
}
