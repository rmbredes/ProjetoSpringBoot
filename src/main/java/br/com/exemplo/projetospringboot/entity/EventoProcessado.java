package br.com.exemplo.projetospringboot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * Representa o registro de um evento já processado
 * por determinado consumer.
 *
 * Essa entidade será usada para impedir que o mesmo
 * consumer aplique duas vezes o efeito de um evento.
 */
@Entity
@Table(
        name = "eventos_processados",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_evento_consumer",
                        columnNames = {
                                "evento_id",
                                "nome_consumer"
                        }
                )
        }
)
public class EventoProcessado {

    /*
     * Identificador interno da tabela.
     *
     * Não é o identificador do evento Kafka.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Identificador de negócio recebido dentro do evento.
     *
     * Ele permanece igual mesmo quando o Kafka entrega
     * o mesmo evento novamente.
     */
    @Column(
            name = "evento_id",
            nullable = false,
            updatable = false
    )
    private UUID eventoId;

    /*
     * Identifica qual consumer processou o evento.
     *
     * Isso permite que estoque e notificação processem
     * independentemente o mesmo eventoId.
     */
    @Column(
            name = "nome_consumer",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String nomeConsumer;

    /*
     * Registra quando o processamento foi concluído.
     */
    @Column(
            name = "processado_em",
            nullable = false,
            updatable = false
    )
    private Instant processadoEm;

    /**
     * Construtor sem argumentos exigido pelo JPA.
     */
    public EventoProcessado() {
    }

    /**
     * Cria o registro de um evento processado.
     *
     * @param eventoId identificador único do evento
     * @param nomeConsumer consumer que processou o evento
     * @param processadoEm instante da conclusão do processamento
     */
    public EventoProcessado(
            UUID eventoId,
            String nomeConsumer,
            Instant processadoEm
    ) {
        this.eventoId = eventoId;
        this.nomeConsumer = nomeConsumer;
        this.processadoEm = processadoEm;
    }

    /**
     * Retorna o identificador interno da tabela.
     *
     * @return identificador gerado pelo banco
     */
    public Long getId() {
        return id;
    }

    /**
     * Retorna o identificador do evento processado.
     *
     * @return identificador do evento
     */
    public UUID getEventoId() {
        return eventoId;
    }

    /**
     * Retorna o nome do consumer que processou o evento.
     *
     * @return nome do consumer
     */
    public String getNomeConsumer() {
        return nomeConsumer;
    }

    /**
     * Retorna o instante em que o evento foi processado.
     *
     * @return instante do processamento
     */
    public Instant getProcessadoEm() {
        return processadoEm;
    }
}