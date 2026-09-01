package br.com.exemplo.projetospringboot.entity;

import jakarta.persistence.*;

/**
 * Representa um cliente persistido na tabela clientes.
 */
@Entity
@Table(name = "clientes")
public class Cliente {

    /** Identificador gerado automaticamente pelo banco. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nome utilizado para identificar o cliente. */
    @Column(nullable = false)
    private String nome;

    /** E-mail único utilizado como contato do cliente. */
    @Column(nullable = false, unique = true)
    private String email;

    /** Indica se o cliente permanece ativo no sistema. */
    @Column(nullable = false)
    private boolean ativo;

    /**
     * Construtor exigido pelo JPA para reconstruir a entidade.
     */
    public Cliente() {
    }

    /**
     * Cria uma entidade com todos os seus dados.
     *
     * @param id identificador da entidade
     * @param nome nome do cliente
     * @param email e-mail único
     * @param ativo situação atual
     */
    public Cliente(
            Long id,
            String nome,
            String email,
            boolean ativo
    ) {
        /* Copia os valores recebidos para o estado da entidade. */
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.ativo = ativo;
    }

    /** @return identificador do cliente */
    public Long getId() {
        return id;
    }

    /** @param id novo identificador do cliente */
    public void setId(Long id) {
        this.id = id;
    }

    /** @return nome do cliente */
    public String getNome() {
        return nome;
    }

    /** @param nome novo nome do cliente */
    public void setNome(String nome) {
        this.nome = nome;
    }

    /** @return e-mail do cliente */
    public String getEmail() {
        return email;
    }

    /** @param email novo e-mail do cliente */
    public void setEmail(String email) {
        this.email = email;
    }

    /** @return true quando o cliente está ativo */
    public boolean isAtivo() {
        return ativo;
    }

    /** @param ativo nova situação do cliente */
    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
