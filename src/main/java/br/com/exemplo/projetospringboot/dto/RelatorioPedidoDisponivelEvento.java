package br.com.exemplo.projetospringboot.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Representa o fato de que um relatório ficou disponível.
 *
 * <p>Diferentemente de {@link GerarRelatorioPedidoMensagem}, que contém
 * um comando enviado ao SQS, este DTO descreve um evento já ocorrido.
 * O SNS distribuirá o mesmo evento para todos os seus assinantes.</p>
 *
 * @param eventoId identificador único do evento publicado
 * @param tipo nome que identifica a natureza do evento
 * @param relatorioId identificador do relatório concluído
 * @param pedidoId identificador do pedido apresentado no relatório
 * @param solicitacaoId identificador único da solicitação original
 * @param objectKey endereço interno do PDF no bucket S3
 * @param ocorridoEm instante em que o relatório ficou disponível
 */
public record RelatorioPedidoDisponivelEvento(
        UUID eventoId,
        String tipo,
        Long relatorioId,
        Long pedidoId,
        UUID solicitacaoId,
        String objectKey,
        Instant ocorridoEm
) {
}
