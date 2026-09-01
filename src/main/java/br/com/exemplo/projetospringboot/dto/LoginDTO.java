package br.com.exemplo.projetospringboot.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Recebe as credenciais utilizadas no processo de autenticação.
 *
 * @param username nome obrigatório do usuário
 * @param senha senha obrigatória enviada pelo cliente
 */
public record LoginDTO(

        @NotBlank(message = "Usuário é obrigatório")
        String username,

        @NotBlank(message = "Senha é obrigatória")
        String senha

) {
}
