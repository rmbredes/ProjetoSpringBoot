package br.com.exemplo.projetospringboot.entity;

import br.com.exemplo.projetospringboot.enums.StatusEventoOutbox;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Representa um evento que precisa ser publicado no Kafka.
 *
 * O registro é salvo no mesmo banco de dados e na mesma transação
 * utilizada para salvar o pedido.
 *
 * Posteriormente, um processo buscará os eventos pendentes,
 * publicará no Kafka e atualizará o status do registro.
 */
@Entity
@Table(
        name = "eventos_outbox",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_eventos_outbox_evento_id",
                        columnNames = "evento_id"
                )
        }
)
public class EventoOutbox {

    /**
     * Identificador interno do registro na tabela de Outbox.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identificador único do evento.
     *
     * Esse mesmo valor será enviado dentro da mensagem Kafka
     * e utilizado pelos consumidores para garantir idempotência.
     */
    @Column(name = "evento_id", nullable = false, updatable = false)
    private UUID eventoId;

    /**
     * Nome que identifica o tipo do evento armazenado.
     *
     * Exemplo: PedidoCriadoEvent.
     */
    @Column(name = "tipo_evento", nullable = false, length = 150)
    private String tipoEvento;

    /**
     * Nome do tópico Kafka para o qual o evento será publicado.
     *
     * Exemplo: pedidos-criados.
     */
    @Column(name = "topico", nullable = false, length = 150)
    private String topico;

    /**
     * Chave que será utilizada na publicação da mensagem Kafka.
     *
     * No nosso caso, será o identificador do pedido.
     */
    @Column(name = "chave_mensagem", nullable = false, length = 100)
    private String chaveMensagem;

    /**
     * Conteúdo completo do evento convertido para JSON.
     *
     * O job recuperará esse texto e o enviará para o Kafka.
     */
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    /**
     * Situação atual do evento dentro do processo de publicação.
     *
     * O EnumType.STRING faz com que valores como PENDENTE e PUBLICADO
     * sejam armazenados no banco, em vez de números.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StatusEventoOutbox status;

    /**
     * Data e hora em que o registro foi criado.
     */
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    /**
     * Data e hora em que a publicação no Kafka foi confirmada.
     *
     * Permanece nula enquanto o evento não for publicado.
     */
    @Column(name = "publicado_em")
    private Instant publicadoEm;

    /**
     * Quantidade de tentativas de publicação que falharam.
     */
    @Column(name = "quantidade_tentativas", nullable = false)
    private int quantidadeTentativas;

    /**
     * Mensagem referente ao último erro ocorrido durante a publicação.
     *
     * Esse campo ajuda na investigação de problemas.
     */
    @Column(name = "ultimo_erro", length = 2000)
    private String ultimoErro;

    /**
     * Construtor obrigatório para o funcionamento do JPA.
     *
     * Ele não deve ser utilizado diretamente pela aplicação.
     */
    protected EventoOutbox() {
    }

    /**
     * Cria um novo evento pendente de publicação.
     *
     * @param eventoId identificador único do evento
     * @param tipoEvento nome do tipo do evento
     * @param topico tópico Kafka de destino
     * @param chaveMensagem chave da mensagem Kafka
     * @param payload conteúdo do evento convertido para JSON
     */
    public EventoOutbox(
            UUID eventoId,
            String tipoEvento,
            String topico,
            String chaveMensagem,
            String payload
    ) {
        this.eventoId = eventoId;
        this.tipoEvento = tipoEvento;
        this.topico = topico;
        this.chaveMensagem = chaveMensagem;
        this.payload = payload;
        this.status = StatusEventoOutbox.PENDENTE;
        this.criadoEm = Instant.now();
        this.quantidadeTentativas = 0;
    }


    /**
     * Cria um evento pendente preservando o contexto
     * de tracing da operação que o originou.
     *
     * @param eventoId identificador único do evento
     * @param tipoEvento tipo do evento
     * @param topico tópico Kafka de destino
     * @param chaveMensagem chave da mensagem
     * @param payload conteúdo JSON
     * @param traceParent contexto W3C do trace de origem
     * @param traceState estado adicional opcional do trace
     */
    public EventoOutbox(
            UUID eventoId,
            String tipoEvento,
            String topico,
            String chaveMensagem,
            String payload,
            String traceParent,
            String traceState
    ) {
        this.eventoId = eventoId;
        this.tipoEvento = tipoEvento;
        this.topico = topico;
        this.chaveMensagem = chaveMensagem;
        this.payload = payload;
        this.traceParent = traceParent;
        this.traceState = traceState;
        this.status = StatusEventoOutbox.PENDENTE;
        this.criadoEm = Instant.now();
        this.quantidadeTentativas = 0;
    }


    /**
     * Marca o evento como publicado depois que o Kafka
     * confirmar o recebimento da mensagem.
     */
    public void marcarComoPublicado() {
        this.status = StatusEventoOutbox.PUBLICADO;
        this.publicadoEm = Instant.now();
        this.ultimoErro = null;
    }

    /**
     * Registra uma falha ocorrida durante a publicação.
     *
     * Quando o limite de tentativas for atingido,
     * o evento passa para o status ERRO.
     *
     * @param mensagemErro descrição do erro ocorrido
     * @param limiteTentativas quantidade máxima de tentativas permitidas
     */
    public void registrarFalha(
            String mensagemErro,
            int limiteTentativas
    ) {
        this.quantidadeTentativas++;
        this.ultimoErro = mensagemErro;

        if (this.quantidadeTentativas >= limiteTentativas) {
            this.status = StatusEventoOutbox.ERRO;
        }
    }

    /**
     * Retorna o identificador interno do registro.
     *
     * @return identificador gerado pelo banco
     */
    public Long getId() {
        return id;
    }

    /**
     * Retorna o identificador único do evento.
     *
     * @return UUID do evento
     */
    public UUID getEventoId() {
        return eventoId;
    }

    /**
     * Retorna o nome do tipo do evento.
     *
     * @return tipo do evento
     */
    public String getTipoEvento() {
        return tipoEvento;
    }

    /**
     * Retorna o tópico Kafka de destino.
     *
     * @return nome do tópico
     */
    public String getTopico() {
        return topico;
    }

    /**
     * Retorna a chave que será utilizada na mensagem Kafka.
     *
     * @return chave da mensagem
     */
    public String getChaveMensagem() {
        return chaveMensagem;
    }

    /**
     * Retorna o conteúdo JSON armazenado.
     *
     * @return payload do evento
     */
    public String getPayload() {
        return payload;
    }

    /**
     * Contexto W3C que identifica o trace e o span de origem.
     *
     * Permanece nulo para eventos antigos ou criados sem
     * um contexto de tracing ativo.
     */
    @Column(name = "trace_parent", length = 255)
    private String traceParent;

    /**
     * Informações adicionais opcionais do contexto W3C.
     */
    @Column(name = "trace_state", length = 512)
    private String traceState;

    /**
     * Retorna a situação atual da publicação.
     *
     * @return status do evento
     */
    public StatusEventoOutbox getStatus() {
        return status;
    }

    /**
     * Retorna a data em que o evento foi criado.
     *
     * @return data de criação
     */
    public Instant getCriadoEm() {
        return criadoEm;
    }

    /**
     * Retorna a data em que o Kafka confirmou a publicação.
     *
     * @return data de publicação ou null quando ainda não publicado
     */
    public Instant getPublicadoEm() {
        return publicadoEm;
    }

    /**
     * Retorna a quantidade de falhas de publicação.
     *
     * @return número de tentativas que falharam
     */
    public int getQuantidadeTentativas() {
        return quantidadeTentativas;
    }

    /**
     * Retorna a descrição do último erro ocorrido.
     *
     * @return mensagem do último erro ou null
     */
    public String getUltimoErro() {
        return ultimoErro;
    }

    /**
     * @return contexto W3C do trace de origem
     */
    public String getTraceParent() {
        return traceParent;
    }

    /**
     * @return estado adicional do trace de origem
     */
    public String getTraceState() {
        return traceState;
    }
}