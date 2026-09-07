package br.com.exemplo.projetospringboot.entity;

import br.com.exemplo.projetospringboot.enums.StatusRelatorioPedido;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * Representa uma solicitação de geração de relatório para um pedido.
 *
 * <p>O banco não armazenará os bytes do PDF. Assim como acontece com
 * os anexos, o arquivo será guardado no Amazon S3 e esta entidade
 * manterá apenas os metadados necessários para localizá-lo.</p>
 */
@Entity
@Table(
        name = "relatorios_pedido",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_relatorios_pedido_solicitacao_id",
                        columnNames = "solicitacao_id"
                ),
                @UniqueConstraint(
                        name = "uk_relatorios_pedido_object_key",
                        columnNames = "object_key"
                )
        },
        indexes = {
                @Index(
                        name = "idx_relatorios_pedido_pedido_status",
                        columnList = "pedido_id, status"
                )
        }
)
public class RelatorioPedido {

    /** Identificador interno gerado pelo banco de dados. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identificador público e imutável da solicitação.
     *
     * <p>Esse UUID também será transportado na mensagem SQS e servirá
     * para reconhecer uma eventual entrega repetida.</p>
     */
    @Column(
            name = "solicitacao_id",
            nullable = false,
            updatable = false
    )
    private UUID solicitacaoId;

    /** Pedido cujos dados serão apresentados no relatório. */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "pedido_id",
            nullable = false,
            updatable = false
    )
    private Pedido pedido;

    /** Situação atual do processamento assíncrono. */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private StatusRelatorioPedido status;

    /**
     * Key utilizada para encontrar o PDF dentro do bucket S3.
     *
     * <p>Permanece nula até a conclusão da geração.</p>
     */
    @Column(
            name = "object_key",
            length = 1024
    )
    private String objectKey;

    /** Nome amigável apresentado no download do arquivo. */
    @Column(
            name = "nome_arquivo",
            length = 255
    )
    private String nomeArquivo;

    /** Tipo MIME do relatório; para o nosso fluxo será application/pdf. */
    @Column(
            name = "content_type",
            length = 100
    )
    private String contentType;

    /** Tamanho exato do PDF armazenado no S3. */
    @Column(name = "tamanho_bytes")
    private Long tamanhoBytes;

    /** Hash SHA-256 calculado sobre os bytes do PDF. */
    @Column(
            name = "checksum_sha256",
            length = 64
    )
    private String checksumSha256;

    /** ETag devolvido pelo S3 depois do upload. */
    @Column(
            name = "etag",
            length = 255
    )
    private String eTag;

    /** Quantidade de falhas registradas durante o fluxo. */
    @Column(
            name = "quantidade_tentativas",
            nullable = false
    )
    private int quantidadeTentativas;

    /** Descrição da falha mais recente, quando existir. */
    @Column(
            name = "ultimo_erro",
            length = 2000
    )
    private String ultimoErro;

    /** Momento em que a solicitação foi criada. */
    @Column(
            name = "criado_em",
            nullable = false,
            updatable = false
    )
    private Instant criadoEm;

    /** Momento da última alteração de estado. */
    @Column(
            name = "atualizado_em",
            nullable = false
    )
    private Instant atualizadoEm;

    /** Momento em que o PDF ficou disponível no S3. */
    @Column(name = "concluido_em")
    private Instant concluidoEm;

    /** Construtor sem argumentos exigido pelo JPA. */
    protected RelatorioPedido() {
    }

    /**
     * Cria uma solicitação que ainda precisa ser publicada no SQS.
     *
     * @param solicitacaoId identificador único da solicitação
     * @param pedido pedido que dará origem ao relatório
     */
    public RelatorioPedido(
            UUID solicitacaoId,
            Pedido pedido
    ) {
        this.solicitacaoId = exigirValor(
                solicitacaoId,
                "solicitacaoId"
        );
        this.pedido = exigirValor(
                pedido,
                "pedido"
        );
        this.status = StatusRelatorioPedido.PENDENTE;
        this.quantidadeTentativas = 0;

        Instant agora = Instant.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    /**
     * Registra que o SQS confirmou o recebimento da mensagem.
     */
    public void marcarComoEnfileirado() {
        exigirStatusAtual(StatusRelatorioPedido.PENDENTE);

        this.status = StatusRelatorioPedido.ENFILEIRADO;
        this.ultimoErro = null;
        this.atualizadoEm = Instant.now();
    }

    /**
     * Registra a conclusão da geração e do upload para o S3.
     *
     * @param objectKey key que identifica o PDF no bucket
     * @param nomeArquivo nome amigável utilizado no download
     * @param tamanhoBytes tamanho exato do PDF
     * @param checksumSha256 hash SHA-256 do conteúdo
     * @param eTag identificador devolvido pelo S3
     */
    public void marcarComoDisponivel(
            String objectKey,
            String nomeArquivo,
            long tamanhoBytes,
            String checksumSha256,
            String eTag
    ) {
        exigirStatusAtual(StatusRelatorioPedido.ENFILEIRADO);

        if (tamanhoBytes <= 0) {
            throw new IllegalArgumentException(
                    "tamanhoBytes deve ser maior que zero"
            );
        }

        this.objectKey = exigirTexto(objectKey, "objectKey");
        this.nomeArquivo = exigirTexto(nomeArquivo, "nomeArquivo");
        this.contentType = "application/pdf";
        this.tamanhoBytes = tamanhoBytes;
        this.checksumSha256 = exigirTexto(
                checksumSha256,
                "checksumSha256"
        );
        this.eTag = exigirTexto(eTag, "eTag");
        this.status = StatusRelatorioPedido.DISPONIVEL;
        this.ultimoErro = null;

        Instant agora = Instant.now();
        this.atualizadoEm = agora;
        this.concluidoEm = agora;
    }

    /**
     * Registra uma falha definitiva no envio ou no processamento.
     *
     * @param mensagemErro descrição legível da falha
     */
    public void marcarComoFalha(String mensagemErro) {
        if (status == StatusRelatorioPedido.DISPONIVEL) {
            throw new IllegalStateException(
                    "Um relatório disponível não pode ser marcado como falha"
            );
        }

        this.quantidadeTentativas++;
        this.ultimoErro = exigirTexto(
                mensagemErro,
                "mensagemErro"
        );
        this.status = StatusRelatorioPedido.FALHA;
        this.atualizadoEm = Instant.now();
    }

    /** Exige que a entidade esteja no ponto correto do fluxo. */
    private void exigirStatusAtual(StatusRelatorioPedido esperado) {
        if (status != esperado) {
            throw new IllegalStateException(
                    "Estado atual do relatório deveria ser "
                            + esperado
                            + ", mas é "
                            + status
            );
        }
    }

    /** Valida textos obrigatórios recebidos pelos métodos de domínio. */
    private static String exigirTexto(
            String valor,
            String nomeCampo
    ) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    nomeCampo + " não pode ser vazio"
            );
        }

        return valor;
    }

    /** Valida referências obrigatórias recebidas pelo construtor. */
    private static <T> T exigirValor(
            T valor,
            String nomeCampo
    ) {
        if (valor == null) {
            throw new IllegalArgumentException(
                    nomeCampo + " não pode ser nulo"
            );
        }

        return valor;
    }

    /** @return identificador interno do relatório */
    public Long getId() {
        return id;
    }

    /** @return identificador público da solicitação */
    public UUID getSolicitacaoId() {
        return solicitacaoId;
    }

    /** @return pedido que originou o relatório */
    public Pedido getPedido() {
        return pedido;
    }

    /** @return estado atual da geração */
    public StatusRelatorioPedido getStatus() {
        return status;
    }

    /** @return key do PDF no S3 ou null enquanto não concluído */
    public String getObjectKey() {
        return objectKey;
    }

    /** @return nome amigável do arquivo */
    public String getNomeArquivo() {
        return nomeArquivo;
    }

    /** @return tipo MIME do arquivo */
    public String getContentType() {
        return contentType;
    }

    /** @return tamanho do arquivo em bytes */
    public Long getTamanhoBytes() {
        return tamanhoBytes;
    }

    /** @return hash SHA-256 do arquivo */
    public String getChecksumSha256() {
        return checksumSha256;
    }

    /** @return ETag devolvido pelo S3 */
    public String getETag() {
        return eTag;
    }

    /** @return quantidade de falhas registradas */
    public int getQuantidadeTentativas() {
        return quantidadeTentativas;
    }

    /** @return descrição da última falha */
    public String getUltimoErro() {
        return ultimoErro;
    }

    /** @return momento em que a solicitação foi criada */
    public Instant getCriadoEm() {
        return criadoEm;
    }

    /** @return momento da última alteração */
    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    /** @return momento da conclusão ou null enquanto não concluído */
    public Instant getConcluidoEm() {
        return concluidoEm;
    }
}
