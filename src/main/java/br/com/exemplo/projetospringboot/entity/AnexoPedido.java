package br.com.exemplo.projetospringboot.entity;

import br.com.exemplo.projetospringboot.enums.StatusAnexoPedido;
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

/**
 * Representa os metadados de um anexo pertencente a um pedido.
 *
 * <p>Esta entidade não armazena os bytes do arquivo. O conteúdo real
 * permanece no Amazon S3 e o banco guarda somente as informações
 * necessárias para localizar, validar e apresentar o anexo.</p>
 */
@Entity
@Table(
        name = "anexos_pedido",

        /*
         * Garante no banco que uma mesma key do S3 não seja associada
         * a dois registros diferentes.
         */
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_anexos_pedido_object_key",
                        columnNames = "object_key"
                )
        },

        /*
         * Facilita a consulta dos anexos de determinado pedido
         * filtrados por status.
         */
        indexes = {
                @Index(
                        name = "idx_anexos_pedido_pedido_status",
                        columnList = "pedido_id, status"
                )
        }
)
public class AnexoPedido {

    /**
     * Identificador interno gerado pelo banco de dados.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Pedido ao qual o anexo pertence.
     *
     * <p>FetchType.LAZY evita carregar o pedido automaticamente
     * quando precisamos consultar somente o anexo.</p>
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "pedido_id",
            nullable = false
    )
    private Pedido pedido;

    /**
     * Key completa que identifica o objeto dentro do bucket.
     *
     * <p>Exemplo:</p>
     *
     * <pre>
     * pedidos/15/anexos/550e8400-e29b-41d4-a716-446655440000
     * </pre>
     *
     * <p>Ela não poderá ser alterada depois que o registro
     * for criado.</p>
     */
    @Column(
            name = "object_key",
            nullable = false,
            updatable = false,
            length = 1024
    )
    private String objectKey;

    /**
     * Nome original recebido do usuário.
     *
     * <p>Esse nome será utilizado na apresentação e no download,
     * mas não será utilizado diretamente como key do S3.</p>
     */
    @Column(
            name = "nome_original",
            nullable = false,
            length = 255
    )
    private String nomeOriginal;

    /**
     * Tipo MIME informado para o arquivo.
     *
     * <p>Exemplos:</p>
     *
     * <ul>
     *     <li>application/pdf;</li>
     *     <li>image/png;</li>
     *     <li>image/jpeg;</li>
     *     <li>text/plain.</li>
     * </ul>
     */
    @Column(
            name = "content_type",
            nullable = false,
            length = 150
    )
    private String contentType;

    /**
     * Tamanho exato do arquivo em bytes.
     */
    @Column(
            name = "tamanho_bytes",
            nullable = false
    )
    private long tamanhoBytes;

    /**
     * Hash SHA-256 calculado pela aplicação.
     *
     * <p>Um SHA-256 representado em hexadecimal ocupa
     * exatamente 64 caracteres.</p>
     */
    @Column(
            name = "checksum_sha256",
            nullable = false,
            length = 64
    )
    private String checksumSha256;

    /**
     * ETag devolvido pelo Amazon S3 depois do upload.
     *
     * <p>Permanece nulo enquanto o anexo estiver PENDENTE
     * ou quando o envio tiver falhado.</p>
     */
    @Column(
            name = "etag",
            length = 255
    )
    private String eTag;

    /**
     * Estado atual do armazenamento.
     *
     * <p>EnumType.STRING grava nomes legíveis, como PENDENTE
     * e DISPONIVEL, em vez de números dependentes da ordem
     * das constantes do enum.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private StatusAnexoPedido status;

    /**
     * Mensagem referente ao último erro de armazenamento.
     *
     * <p>Permanece nula quando não ocorreu uma falha.</p>
     */
    @Column(
            name = "ultimo_erro",
            length = 2000
    )
    private String ultimoErro;

    /**
     * Momento em que o registro foi criado.
     */
    @Column(
            name = "criado_em",
            nullable = false,
            updatable = false
    )
    private Instant criadoEm;

    /**
     * Momento da última mudança de estado.
     */
    @Column(
            name = "atualizado_em",
            nullable = false
    )
    private Instant atualizadoEm;

    /**
     * Construtor exigido pelo JPA.
     *
     * <p>Protected permite que o Hibernate utilize o construtor,
     * mas evita seu uso indiscriminado por outras classes.</p>
     */
    protected AnexoPedido() {
    }

    /**
     * Cria um anexo aguardando o upload para o S3.
     *
     * @param pedido pedido proprietário do anexo
     * @param objectKey key única que será utilizada no S3
     * @param nomeOriginal nome recebido do usuário
     * @param contentType tipo MIME do conteúdo
     * @param tamanhoBytes tamanho do arquivo
     * @param checksumSha256 hash SHA-256 calculado pela aplicação
     */
    public AnexoPedido(
            Pedido pedido,
            String objectKey,
            String nomeOriginal,
            String contentType,
            long tamanhoBytes,
            String checksumSha256
    ) {
        /*
         * A entidade protege suas regras básicas mesmo que seja
         * criada fora de um controller ou serviço HTTP.
         */
        this.pedido = exigirValor(pedido, "pedido");
        this.objectKey = exigirTexto(objectKey, "objectKey");
        this.nomeOriginal = exigirTexto(nomeOriginal, "nomeOriginal");
        this.contentType = exigirTexto(contentType, "contentType");
        this.checksumSha256 = exigirTexto(
                checksumSha256,
                "checksumSha256"
        );

        if (tamanhoBytes <= 0) {
            throw new IllegalArgumentException(
                    "tamanhoBytes deve ser maior que zero"
            );
        }

        this.tamanhoBytes = tamanhoBytes;
        this.status = StatusAnexoPedido.PENDENTE;

        Instant agora = Instant.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    /**
     * Registra que o S3 confirmou o armazenamento.
     *
     * @param eTag identificador devolvido pelo S3
     */
    public void marcarComoDisponivel(String eTag) {
        exigirStatusAtual(StatusAnexoPedido.PENDENTE);

        this.eTag = exigirTexto(eTag, "eTag");
        this.status = StatusAnexoPedido.DISPONIVEL;
        this.ultimoErro = null;
        this.atualizadoEm = Instant.now();
    }

    /**
     * Registra que ocorreu uma falha durante o upload.
     *
     * @param mensagemErro descrição da falha
     */
    public void marcarComoFalha(String mensagemErro) {
        exigirStatusAtual(StatusAnexoPedido.PENDENTE);

        this.status = StatusAnexoPedido.FALHA;
        this.ultimoErro = exigirTexto(
                mensagemErro,
                "mensagemErro"
        );
        this.atualizadoEm = Instant.now();
    }

    /**
     * Registra que o objeto foi removido do S3.
     */
    public void marcarComoExcluido() {
        exigirStatusAtual(StatusAnexoPedido.DISPONIVEL);

        this.status = StatusAnexoPedido.EXCLUIDO;
        this.atualizadoEm = Instant.now();
    }

    /**
     * Impede uma transição inválida de estado.
     *
     * <p>Por exemplo, um anexo que já foi excluído não pode voltar
     * diretamente para DISPONIVEL.</p>
     *
     * @param statusEsperado estado exigido pela operação
     */
    private void exigirStatusAtual(StatusAnexoPedido statusEsperado) {
        if (status != statusEsperado) {
            throw new IllegalStateException(
                    "Estado atual do anexo deveria ser "
                            + statusEsperado
                            + ", mas é "
                            + status
            );
        }
    }

    /**
     * Valida argumentos textuais obrigatórios.
     *
     * @param valor texto recebido
     * @param nomeCampo nome usado na mensagem de erro
     * @return o próprio texto validado
     */
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

    /**
     * Valida referências obrigatórias.
     *
     * @param valor objeto recebido
     * @param nomeCampo nome usado na mensagem de erro
     * @return o próprio objeto validado
     * @param <T> tipo do objeto validado
     */
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

    /** @return identificador interno do anexo */
    public Long getId() {
        return id;
    }

    /** @return pedido ao qual o anexo pertence */
    public Pedido getPedido() {
        return pedido;
    }

    /** @return key utilizada para localizar o objeto no S3 */
    public String getObjectKey() {
        return objectKey;
    }

    /** @return nome original recebido do usuário */
    public String getNomeOriginal() {
        return nomeOriginal;
    }

    /** @return tipo MIME do arquivo */
    public String getContentType() {
        return contentType;
    }

    /** @return tamanho do arquivo em bytes */
    public long getTamanhoBytes() {
        return tamanhoBytes;
    }

    /** @return hash SHA-256 calculado pela aplicação */
    public String getChecksumSha256() {
        return checksumSha256;
    }

    /** @return ETag devolvido pelo S3 ou null antes do upload */
    public String getETag() {
        return eTag;
    }

    /** @return estado atual do anexo */
    public StatusAnexoPedido getStatus() {
        return status;
    }

    /** @return mensagem do último erro ou null */
    public String getUltimoErro() {
        return ultimoErro;
    }

    /** @return momento de criação */
    public Instant getCriadoEm() {
        return criadoEm;
    }

    /** @return momento da última mudança de estado */
    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}