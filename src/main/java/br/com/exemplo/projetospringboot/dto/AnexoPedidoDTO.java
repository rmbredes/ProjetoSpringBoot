package br.com.exemplo.projetospringboot.dto;

import br.com.exemplo.projetospringboot.enums.StatusAnexoPedido;

import java.time.Instant;

/**
 * Representa os dados públicos de um anexo de pedido.
 *
 * <p>Não expomos a objectKey nem o ETag porque são detalhes internos
 * da integração entre a aplicação e o Amazon S3.</p>
 *
 * @param id identificador do anexo
 * @param pedidoId identificador do pedido proprietário
 * @param nomeOriginal nome apresentado ao usuário
 * @param contentType tipo MIME do arquivo
 * @param tamanhoBytes tamanho do conteúdo
 * @param checksumSha256 hash utilizado para verificar a integridade
 * @param status situação atual do anexo
 * @param criadoEm momento de criação do registro
 */
public record AnexoPedidoDTO(
        Long id,
        Long pedidoId,
        String nomeOriginal,
        String contentType,
        long tamanhoBytes,
        String checksumSha256,
        StatusAnexoPedido status,
        Instant criadoEm
) {
}