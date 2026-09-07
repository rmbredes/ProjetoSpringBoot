package br.com.exemplo.projetospringboot.dto;

import br.com.exemplo.projetospringboot.enums.StatusRelatorioPedido;

import java.time.Instant;
import java.util.UUID;

/**
 * Transporta pela API o estado de uma solicitação de relatório.
 *
 * <p>O objectKey não é exposto. Para baixar o PDF, o cliente utilizará
 * um endpoint específico que produzirá uma URL temporária do S3.</p>
 *
 * @param id identificador interno do relatório
 * @param solicitacaoId UUID usado para rastrear a solicitação
 * @param pedidoId pedido que originou o relatório
 * @param status situação atual da geração
 * @param nomeArquivo nome do PDF quando estiver disponível
 * @param contentType tipo MIME do arquivo
 * @param tamanhoBytes tamanho do PDF
 * @param checksumSha256 hash do conteúdo gerado
 * @param quantidadeTentativas quantidade de falhas registradas
 * @param ultimoErro descrição da última falha
 * @param criadoEm momento da solicitação
 * @param atualizadoEm momento da última alteração
 * @param concluidoEm momento da conclusão
 */
public record RelatorioPedidoDTO(
        Long id,
        UUID solicitacaoId,
        Long pedidoId,
        StatusRelatorioPedido status,
        String nomeArquivo,
        String contentType,
        Long tamanhoBytes,
        String checksumSha256,
        int quantidadeTentativas,
        String ultimoErro,
        Instant criadoEm,
        Instant atualizadoEm,
        Instant concluidoEm
) {
}
