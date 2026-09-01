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

/**
 * Executa o login e o cadastro de usuários aplicando as regras de segurança da aplicação.
 */
@Service
public class AuthService {

    /** Componente do Spring Security que valida usuário e senha. */
    private final AuthenticationManager authenticationManager;

    /** Serviço responsável por gerar o JWT após a autenticação. */
    private final JwtService jwtService;

    /** Repositório usado para consultar e salvar usuários. */
    private final UsuarioRepository usuarioRepository;

    /** Codificador usado para nunca armazenar a senha em texto puro. */
    private final PasswordEncoder passwordEncoder;

    /**
     * Recebe todos os componentes necessários para autenticação e cadastro.
     */
    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        // Guarda o autenticador configurado pelo Spring Security.
        this.authenticationManager =
                authenticationManager;

        // Guarda o gerador de tokens JWT.
        this.jwtService =
                jwtService;

        // Guarda o acesso aos dados dos usuários.
        this.usuarioRepository =
                usuarioRepository;

        // Guarda o codificador que protegerá as senhas cadastradas.
        this.passwordEncoder =
                passwordEncoder;
    }

    /*
     * =========================================================
     * LOGIN
     * =========================================================
     */

    /**
     * Valida as credenciais e devolve um token para as próximas requisições.
     *
     * @param loginDTO usuário e senha informados
     * @return dados do token JWT gerado
     */
    public TokenDTO login(
            LoginDTO loginDTO
    ) {

        // Converte as credenciais recebidas para o formato do Spring Security.
        UsernamePasswordAuthenticationToken
                authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        loginDTO.username(),
                        loginDTO.senha()
                );

        // Solicita a validação do usuário e da senha.
        Authentication authentication =
                authenticationManager.authenticate(
                        authenticationToken
                );

        // Gera o JWT somente depois que as credenciais foram confirmadas.
        String jwt =
                jwtService.gerarToken(
                        authentication
                );

        // Informa o token, seu tipo e seu tempo de validade ao cliente.
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

    /**
     * Cadastra um usuário comum com a senha protegida por hash.
     *
     * @param dto dados recebidos para o cadastro
     * @return dados públicos do usuário salvo
     */
    public UsuarioDTO cadastrar(
            UsuarioDTO dto
    ) {

        // Impede a criação de dois cadastros com o mesmo nome de acesso.
        if (
                usuarioRepository.existsByUsername(
                        dto.username()
                )
        ) {

            throw new IllegalArgumentException(
                    "Usuário já cadastrado"
            );
        }

        // Cria a entidade que será persistida.
        Usuario usuario =
                new Usuario();

        // Copia o nome de acesso recebido.
        usuario.setUsername(
                dto.username()
        );

        // Codifica a senha antes que ela chegue ao banco de dados.
        usuario.setSenha(
                passwordEncoder.encode(
                        dto.senha()
                )
        );

        /*
         * Por segurança não confiamos em uma role
         * ADMIN enviada pelo cliente.
         */
        // Todo cadastro público nasce com a permissão de usuário comum.
        usuario.setRole(
                "USER"
        );

        // Persiste o usuário com a senha já protegida.
        Usuario salvo =
                usuarioRepository.save(
                        usuario
                );

        // Não devolve a senha, nem mesmo em sua forma codificada.
        return new UsuarioDTO(
                salvo.getId(),
                salvo.getUsername(),
                null,
                salvo.getRole()
        );
    }
}
