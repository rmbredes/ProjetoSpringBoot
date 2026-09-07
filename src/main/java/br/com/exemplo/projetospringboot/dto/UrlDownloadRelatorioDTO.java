package br.com.exemplo.projetospringboot.dto;

import java.time.Instant;

/**
 * Transporta a autorização temporária para baixar um relatório do S3.
 *
 * <p>O bucket continua privado. A URL contém uma assinatura com prazo
 * de validade e deixa de funcionar automaticamente após a expiração.</p>
 *
 * @param url endereço temporário de download direto no Amazon S3
 * @param expiraEm instante em que a autorização deixará de funcionar
 */
public record UrlDownloadRelatorioDTO(
        String url,
        Instant expiraEm
) {
}
