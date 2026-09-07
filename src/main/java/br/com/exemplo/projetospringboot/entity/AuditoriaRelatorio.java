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
 * Registra que o evento de disponibilidade de um relatório foi recebido.
 *
 * <p>Esta tabela representa o efeito produzido pelo consumer de auditoria.
 * O evento original permanece identificado por {@code eventoId}.</p>
 */
@Entity
@Table(
        name = "auditorias_relatorio",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_auditoria_relatorio_evento",
                columnNames = "evento_id"
        )
)
public class AuditoriaRelatorio {

    /** Identificador interno gerado pelo banco de dados. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador único recebido no evento SNS. */
    @Column(name = "evento_id", nullable = false, updatable = false)
    private UUID eventoId;

    /** Tipo do evento processado pelo consumer. */
    @Column(nullable = false, updatable = false, length = 100)
    private String tipo;

    /** Identificador do relatório que ficou disponível. */
    @Column(name = "relatorio_id", nullable = false, updatable = false)
    private Long relatorioId;

    /** Identificador do pedido relacionado ao relatório. */
    @Column(name = "pedido_id", nullable = false, updatable = false)
    private Long pedidoId;

    /** Identificador que acompanhou a solicitação desde o SQS. */
    @Column(name = "solicitacao_id", nullable = false, updatable = false)
    private UUID solicitacaoId;

    /** Localização permanente do PDF dentro do bucket S3. */
    @Column(name = "object_key", nullable = false, updatable = false, length = 1024)
    private String objectKey;

    /** Instante informado pela aplicação que publicou o evento. */
    @Column(name = "ocorrido_em", nullable = false, updatable = false)
    private Instant ocorridoEm;

    /** Instante em que o consumer concluiu o registro da auditoria. */
    @Column(name = "recebido_em", nullable = false, updatable = false)
    private Instant recebidoEm;

    /** Construtor sem argumentos exigido pelo JPA. */
    public AuditoriaRelatorio() {
    }

    /** Cria uma auditoria com todos os dados imutáveis do evento. */
    public AuditoriaRelatorio(
            UUID eventoId,
            String tipo,
            Long relatorioId,
            Long pedidoId,
            UUID solicitacaoId,
            String objectKey,
            Instant ocorridoEm,
            Instant recebidoEm
    ) {
        this.eventoId = eventoId;
        this.tipo = tipo;
        this.relatorioId = relatorioId;
        this.pedidoId = pedidoId;
        this.solicitacaoId = solicitacaoId;
        this.objectKey = objectKey;
        this.ocorridoEm = ocorridoEm;
        this.recebidoEm = recebidoEm;
    }

    public Long getId() {
        return id;
    }

    public UUID getEventoId() {
        return eventoId;
    }

    public String getTipo() {
        return tipo;
    }

    public Long getRelatorioId() {
        return relatorioId;
    }

    public Long getPedidoId() {
        return pedidoId;
    }

    public UUID getSolicitacaoId() {
        return solicitacaoId;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public Instant getOcorridoEm() {
        return ocorridoEm;
    }

    public Instant getRecebidoEm() {
        return recebidoEm;
    }
}
