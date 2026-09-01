package br.com.exemplo.projetospringboot.entity;

import jakarta.persistence.*;

/**
 * Representa as credenciais e o perfil de autorização
 * de um usuário persistido na aplicação.
 */
@Entity
@Table(name = "usuarios")
public class Usuario {

    /** Identificador gerado automaticamente pelo banco. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nome único utilizado durante o login. */
    @Column(nullable = false, unique = true)
    private String username;

    /** Senha armazenada de forma codificada. */
    @Column(nullable = false)
    private String senha;

    /** Papel de autorização, como USER ou ADMIN. */
    @Column(nullable = false)
    private String role;

    /** Construtor exigido pelo JPA. */
    public Usuario() {
    }

    /**
     * Cria um usuário com todos os seus dados.
     *
     * @param id identificador
     * @param username nome de autenticação
     * @param senha senha codificada
     * @param role papel de autorização
     */
    public Usuario(
            Long id,
            String username,
            String senha,
            String role
    ) {
        /* Inicializa o estado da entidade com os valores recebidos. */
        this.id = id;
        this.username = username;
        this.senha = senha;
        this.role = role;
    }

    /** @return identificador do usuário */
    public Long getId() {
        return id;
    }

    /** @param id novo identificador */
    public void setId(Long id) {
        this.id = id;
    }

    /** @return nome utilizado no login */
    public String getUsername() {
        return username;
    }

    /** @param username novo nome de login */
    public void setUsername(String username) {
        this.username = username;
    }

    /** @return senha codificada */
    public String getSenha() {
        return senha;
    }

    /** @param senha nova senha codificada */
    public void setSenha(String senha) {
        this.senha = senha;
    }

    /** @return papel de autorização */
    public String getRole() {
        return role;
    }

    /** @param role novo papel de autorização */
    public void setRole(String role) {
        this.role = role;
    }
}
