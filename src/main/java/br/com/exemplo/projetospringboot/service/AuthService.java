package br.com.exemplo.projetospringboot.service;

import br.com.exemplo.projetospringboot.dto.LoginDTO;
import br.com.exemplo.projetospringboot.dto.TokenDTO;
import br.com.exemplo.projetospringboot.dto.UsuarioDTO;

import br.com.exemplo.projetospringboot.entity.Usuario;

import br.com.exemplo.projetospringboot.repository.UsuarioRepository;

import br.com.exemplo.projetospringboot.security.JwtService;

import org.springframework.security.authentication.AuthenticationManager;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;

    private final JwtService jwtService;

    private final UsuarioRepository usuarioRepository;

    private final PasswordEncoder passwordEncoder;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager =
                authenticationManager;

        this.jwtService =
                jwtService;

        this.usuarioRepository =
                usuarioRepository;

        this.passwordEncoder =
                passwordEncoder;
    }

    /*
     * =========================================================
     * LOGIN
     * =========================================================
     */

    public TokenDTO login(
            LoginDTO loginDTO
    ) {

        UsernamePasswordAuthenticationToken
                authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        loginDTO.username(),
                        loginDTO.senha()
                );

        Authentication authentication =
                authenticationManager.authenticate(
                        authenticationToken
                );

        String jwt =
                jwtService.gerarToken(
                        authentication
                );

        return new TokenDTO(
                jwt,
                "Bearer",
                jwtService.getExpiration()
        );
    }

    /*
     * =========================================================
     * CADASTRO DE USUÁRIO
     * =========================================================
     */

    public UsuarioDTO cadastrar(
            UsuarioDTO dto
    ) {

        if (
                usuarioRepository.existsByUsername(
                        dto.username()
                )
        ) {

            throw new IllegalArgumentException(
                    "Usuário já cadastrado"
            );
        }

        Usuario usuario =
                new Usuario();

        usuario.setUsername(
                dto.username()
        );

        usuario.setSenha(
                passwordEncoder.encode(
                        dto.senha()
                )
        );

        /*
         * Por segurança não confiamos em uma role
         * ADMIN enviada pelo cliente.
         */
        usuario.setRole(
                "USER"
        );

        Usuario salvo =
                usuarioRepository.save(
                        usuario
                );

        return new UsuarioDTO(
                salvo.getId(),
                salvo.getUsername(),
                null,
                salvo.getRole()
        );
    }
}