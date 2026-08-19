package br.com.exemplo.projetospringboot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioDTO(
        Long id,

        @NotBlank(message = "Usuário é obrigatório")
        String username,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, message = "A senha deve possuir pelo menos 6 caracteres")
        String senha,

        String role
) {
}
