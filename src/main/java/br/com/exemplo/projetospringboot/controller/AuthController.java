package br.com.exemplo.projetospringboot.controller;

import br.com.exemplo.projetospringboot.dto.LoginDTO;
import br.com.exemplo.projetospringboot.dto.TokenDTO;
import br.com.exemplo.projetospringboot.dto.UsuarioDTO;

import br.com.exemplo.projetospringboot.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

/**
 * Expõe os endpoints públicos usados para login e cadastro de usuários.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    /** Serviço que contém as regras de autenticação e cadastro. */
    private final AuthService service;

    /**
     * @param service serviço de autenticação injetado pelo Spring
     */
    public AuthController(
            AuthService service
    ) {
        // Guarda o serviço que atenderá as requisições deste controlador.
        this.service = service;
    }

    /*
     * =========================================================
     * LOGIN
     * =========================================================
     */

    /**
     * Valida as credenciais e devolve um JWT.
     *
     * @param dto dados de acesso validados pelo Bean Validation
     * @return resposta 200 com o token gerado
     */
    @PostMapping("/login")
    public ResponseEntity<TokenDTO> login(
            @Valid
            @RequestBody
            LoginDTO dto
    ) {

        // Encaminha as credenciais ao serviço e monta a resposta de sucesso.
        return ResponseEntity.ok(
                service.login(dto)
        );
    }

    /*
     * =========================================================
     * CADASTRO
     * =========================================================
     */

    /**
     * Cria um usuário comum para acesso à aplicação.
     *
     * @param dto dados de cadastro validados
     * @return resposta 201 com os dados públicos do usuário
     */
    @PostMapping("/cadastrar")
    public ResponseEntity<UsuarioDTO> cadastrar(
            @Valid
            @RequestBody
            UsuarioDTO dto
    ) {

        // Executa o cadastro e informa que um novo recurso foi criado.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        service.cadastrar(dto)
                );
    }
}
