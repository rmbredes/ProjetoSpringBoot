package br.com.exemplo.projetospringboot.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.io.Serializable;

/**
 * Transporta os dados de cliente entre a API e a camada de serviço.
 *
 * @param id identificador gerado pelo banco
 * @param nome nome obrigatório do cliente
 * @param email endereço eletrônico obrigatório e validado
 * @param ativo indica se o cliente pode ser utilizado
 */
public record ClienteDTO(
        Long id,

        @NotBlank(message = "O nome é obrigatório")
        String nome,

        @NotBlank(message = "O email é obrigatório")
        @Email(message = "Email inválido")
        String email,

        boolean ativo
) implements Serializable {
}
