package br.com.exemplo.projetospringboot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Transporta os dados utilizados no cadastro e na consulta de usuários.
 *
 * @param id identificador gerado pelo banco
 * @param username nome utilizado para autenticação
 * @param senha senha recebida no cadastro
 * @param role perfil de autorização associado ao usuário
 */
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
