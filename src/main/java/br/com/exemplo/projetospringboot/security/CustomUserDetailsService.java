package br.com.exemplo.projetospringboot.security;

import br.com.exemplo.projetospringboot.entity.Usuario;
import br.com.exemplo.projetospringboot.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Adapta os usuários do banco ao formato esperado pelo Spring Security.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {
    /** Repositório usado para localizar o usuário que está tentando entrar. */
    private final UsuarioRepository usuarioRepository;

    /**
     * @param usuarioRepository repositório de usuários da aplicação
     */
    public CustomUserDetailsService(
            UsuarioRepository usuarioRepository
    ) {
        // Guarda a dependência para as futuras consultas de autenticação.
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Carrega um usuário e suas permissões pelo nome informado no login.
     *
     * @param username nome de acesso informado
     * @return usuário no formato reconhecido pelo Spring Security
     * @throws UsernameNotFoundException quando o usuário não estiver cadastrado
     */
    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        // Consulta o cadastro e transforma a ausência em uma falha de autenticação clara.
        Usuario usuario = usuarioRepository
                .findByUsername(username)
                .orElseThrow(
                        () -> new UsernameNotFoundException(
                                "Usuário não encontrado"
                        )
                );

        // Constrói o objeto de segurança usando a senha codificada e a role persistida.
        return User
                .withUsername(usuario.getUsername())
                .password(usuario.getSenha())
                .roles(usuario.getRole())
                .build();
    }

}
