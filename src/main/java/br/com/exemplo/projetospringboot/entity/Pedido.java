package br.com.exemplo.projetospringboot.entity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa um pedido associado a um cliente.
 */
@Entity
@Table(name = "pedidos")
public class Pedido {

    /** Identificador gerado automaticamente pelo banco. */
    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    /** Valor monetário registrado para o pedido. */
    private BigDecimal valor;

    /** Data e hora em que o pedido foi criado. */
    private LocalDateTime dataCriacao;

    /** Cliente proprietário do pedido, carregado sob demanda. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    /** Construtor exigido pelo JPA. */
    public Pedido() {
    }

    /**
     * Cria um pedido com todos os seus dados.
     *
     * @param id identificador do pedido
     * @param valor valor monetário
     * @param dataCriacao data da criação
     * @param cliente cliente proprietário
     */
    public Pedido(
            Long id,
            BigDecimal valor,
            LocalDateTime dataCriacao,
            Cliente cliente
    ) {
        /* Inicializa o estado da entidade com os valores recebidos. */
        this.id = id;
        this.valor = valor;
        this.dataCriacao = dataCriacao;
        this.cliente = cliente;
    }

    /** @return identificador do pedido */
    public Long getId() {
        return id;
    }

    /** @param id novo identificador do pedido */
    public void setId(Long id) {
        this.id = id;
    }

    /** @return valor do pedido */
    public BigDecimal getValor() {
        return valor;
    }

    /** @param valor novo valor do pedido */
    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    /** @return data e hora da criação */
    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    /** @param dataCriacao nova data de criação */
    public void setDataCriacao(
            LocalDateTime dataCriacao
    ) {
        this.dataCriacao = dataCriacao;
    }

    /** @return cliente associado ao pedido */
    public Cliente getCliente() {
        return cliente;
    }

    /** @param cliente novo cliente associado */
    public void setCliente(
            Cliente cliente
    ) {
        this.cliente = cliente;
    }

}
