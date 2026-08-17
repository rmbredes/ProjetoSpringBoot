package br.com.exemplo.projetospringboot.repository;


import br.com.exemplo.projetospringboot.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository
        extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteId(
            Long clienteId
    );
}
