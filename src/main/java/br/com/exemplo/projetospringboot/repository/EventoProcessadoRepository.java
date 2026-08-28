package br.com.exemplo.projetospringboot.repository;

import br.com.exemplo.projetospringboot.entity.EventoProcessado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Repository responsável por consultar e persistir
 * os eventos já processados pelos consumers.
 */
public interface EventoProcessadoRepository
        extends JpaRepository<EventoProcessado, Long> {

    /**
     * Conta quantos registros existem para a combinação
     * de evento e consumer informada.
     *
     * Como existe uma restrição única no banco, o resultado
     * esperado será zero ou um.
     *
     * @param eventoId identificador do evento recebido
     * @param nomeConsumer nome do consumer responsável
     * @return quantidade de registros encontrados
     */
    @Query("""
            SELECT COUNT(eventoProcessado)
            FROM EventoProcessado eventoProcessado
            WHERE eventoProcessado.eventoId = :eventoId
              AND eventoProcessado.nomeConsumer = :nomeConsumer
            """)
    long contarPorEventoEConsumer(
            @Param("eventoId") UUID eventoId,
            @Param("nomeConsumer") String nomeConsumer
    );
}