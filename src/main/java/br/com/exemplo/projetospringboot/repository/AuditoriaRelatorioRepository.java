package br.com.exemplo.projetospringboot.repository;

import br.com.exemplo.projetospringboot.entity.AuditoriaRelatorio;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/** Persiste e consulta os eventos processados pela auditoria. */
public interface AuditoriaRelatorioRepository
        extends JpaRepository<AuditoriaRelatorio, Long> {

    /** Verifica idempotência sem carregar todo o registro. */
    boolean existsByEventoId(UUID eventoId);

    /** Lista as auditorias de um pedido, começando pela mais recente. */
    List<AuditoriaRelatorio> findAllByPedidoIdOrderByRecebidoEmDesc(
            Long pedidoId
    );
}
