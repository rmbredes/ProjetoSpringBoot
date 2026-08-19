package br.com.exemplo.projetospringboot.security;

import br.com.exemplo.projetospringboot.entity.Usuario;
import br.com.exemplo.projetospringboot.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarioRepository;

    public CustomUserDetailsService(
            UsuarioRepository usuarioRepository
    ) {
        this.usuarioRepository = usuarioRepository;
    }



    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository
                .findByUsername(username)
                .orElseThrow(
                        () -> new UsernameNotFoundException(
                                "Usuário não encontrado"
                        )
                );

        return User
                .withUsername(usuario.getUsername())
                .password(usuario.getSenha())
                .roles(usuario.getRole())
                .build();
    }

}
