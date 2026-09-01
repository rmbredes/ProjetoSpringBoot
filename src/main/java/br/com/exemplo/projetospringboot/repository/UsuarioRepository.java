package br.com.exemplo.projetospringboot.repository;

import br.com.exemplo.projetospringboot.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Fornece acesso aos usuários cadastrados e às consultas usadas na autenticação.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Procura um usuário pelo nome utilizado no login.
     *
     * @param username nome de acesso
     * @return usuário encontrado, quando existir
     */
    Optional<Usuario> findByUsername(String username);

    /**
     * Verifica se o nome de usuário já foi cadastrado.
     *
     * @param username nome de acesso consultado
     * @return {@code true} quando o usuário já existe
     */
    boolean existsByUsername(String username);
}
