package br.com.exemplo.projetospringboot.dto;

import jakarta.validation.constraints.NotBlank;


public record LoginDTO(

        @NotBlank(message = "Usuário é obrigatório")
        String username,

        @NotBlank(message = "Senha é obrigatória")
        String senha

) {
}
