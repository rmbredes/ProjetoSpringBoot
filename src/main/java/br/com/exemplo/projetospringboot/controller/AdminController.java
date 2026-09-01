package br.com.exemplo.projetospringboot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expõe um endpoint simples utilizado para confirmar
 * se o usuário autenticado possui acesso administrativo.
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    /**
     * Retorna uma mensagem quando a autorização ADMIN for aceita.
     *
     * @return resposta HTTP 200 com a confirmação do acesso
     */
    @GetMapping
    public ResponseEntity<String> admin() {
        /*
         * A autorização é validada pelo SecurityFilterChain
         * antes que a execução chegue a este método.
         */
        return ResponseEntity.ok(
                "Acesso administrativo permitido"
        );
    }
}
