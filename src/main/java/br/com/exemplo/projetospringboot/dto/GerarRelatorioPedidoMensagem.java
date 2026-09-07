package br.com.exemplo.projetospringboot.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Representa o conteúdo enviado para a fila de relatórios do Amazon SQS.
 *
 * <p>Este record é o contrato entre quem solicita o relatório
 * (producer) e quem futuramente o produzirá (consumer).</p>
 *
 * <p>Ele contém apenas identificadores e informações de controle.
 * Os dados completos do pedido não são enviados para a fila, pois o
 * consumer poderá consultá-los no banco no momento do processamento.</p>
 *
 * @param solicitacaoId identificador único usado para evitar duplicidade
 * @param relatorioId identificador do registro salvo em relatorios_pedido
 * @param pedidoId identificador do pedido que aparecerá no relatório
 * @param solicitadoEm instante em que a geração foi solicitada
 */
public record GerarRelatorioPedidoMensagem(
        UUID solicitacaoId,
        Long relatorioId,
        Long pedidoId,
        Instant solicitadoEm
) {
}
