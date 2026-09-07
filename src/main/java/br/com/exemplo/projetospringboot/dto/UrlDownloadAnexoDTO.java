package br.com.exemplo.projetospringboot.dto;

import java.time.Instant;

/**
 * Representa uma autorização temporária para baixar um anexo privado.
 *
 * <p>A URL aponta diretamente para o Amazon S3, mas somente funciona
 * até o instante informado em {@code expiraEm}. A expiração da URL
 * não remove nem modifica o objeto armazenado.</p>
 *
 * @param url endereço temporário assinado pela aplicação
 * @param expiraEm instante UTC em que o endereço perde a validade
 */
public record UrlDownloadAnexoDTO(
        String url,
        Instant expiraEm
) {
}
