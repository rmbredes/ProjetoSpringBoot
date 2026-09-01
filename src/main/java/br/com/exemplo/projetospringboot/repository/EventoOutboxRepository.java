package br.com.exemplo.projetospringboot.repository;

import br.com.exemplo.projetospringboot.entity.EventoOutbox;
import br.com.exemplo.projetospringboot.enums.StatusEventoOutbox;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositório responsável pelo acesso aos eventos armazenados
 * na tabela de Outbox.
 */
public interface EventoOutboxRepository
        extends JpaRepository<EventoOutbox, Long> {

    /**
     * Conta quantos eventos possuem o status informado.
     *
     * Essa consulta será utilizada pela métrica que apresenta
     * a quantidade atual de eventos pendentes na Outbox.
     *
     * @param status situação dos eventos contabilizados
     * @return quantidade atual de eventos com o status
     */
    long countByStatus(
            StatusEventoOutbox status
    );
    /**
     * Busca os eventos que possuem determinado status.
     *
     * Os registros são ordenados pela data de criação para que
     * os eventos mais antigos sejam processados primeiro.
     *
     * O parâmetro Pageable permitirá limitar a quantidade
     * processada em cada execução do job.
     *
     * @param status status dos eventos que devem ser encontrados
     * @param pageable configuração com o limite de registros
     * @return eventos encontrados, ordenados pela data de criação
     */
    @Query("""
            SELECT evento
            FROM EventoOutbox evento
            WHERE evento.status = :status
            ORDER BY evento.criadoEm
            """)
    List<EventoOutbox> buscarPorStatus(
            @Param("status") StatusEventoOutbox status,
            Pageable pageable
    );
}