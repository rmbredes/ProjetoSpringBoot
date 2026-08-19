package br.com.exemplo.projetospringboot.controller;

import br.com.exemplo.projetospringboot.dto.LoginDTO;
import br.com.exemplo.projetospringboot.dto.TokenDTO;
import br.com.exemplo.projetospringboot.dto.UsuarioDTO;

import br.com.exemplo.projetospringboot.service.AuthService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(
            AuthService service
    ) {
        this.service = service;
    }

    /*
     * =========================================================
     * LOGIN
     * =========================================================
     */

    @PostMapping("/login")
    public ResponseEntity<TokenDTO> login(
            @Valid
            @RequestBody
            LoginDTO dto
    ) {

        return ResponseEntity.ok(
                service.login(dto)
        );
    }

    /*
     * =========================================================
     * CADASTRO
     * =========================================================
     */

    @PostMapping("/cadastrar")
    public ResponseEntity<UsuarioDTO> cadastrar(
            @Valid
            @RequestBody
            UsuarioDTO dto
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        service.cadastrar(dto)
                );
    }
}