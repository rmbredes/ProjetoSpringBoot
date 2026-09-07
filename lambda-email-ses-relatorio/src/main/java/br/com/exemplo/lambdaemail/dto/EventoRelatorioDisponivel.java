package br.com.exemplo.lambdaemail.dto;

/**
 * DTO que representa o evento publicado pela aplicação no tópico SNS.
 *
 * <p>O SNS transporta a mensagem como texto JSON. Depois da conversão,
 * cada propriedade fica disponível por meio de um método do record, como
 * {@code evento.pedidoId()} e {@code evento.objectKey()}.</p>
 *
 * @param eventoId identificador único do evento
 * @param tipo nome que identifica o tipo do evento
 * @param relatorioId identificador do relatório no banco de dados
 * @param pedidoId identificador do pedido relacionado ao relatório
 * @param solicitacaoId identificador único da solicitação do relatório
 * @param objectKey endereço do arquivo PDF dentro do bucket S3
 * @param ocorridoEm instante em que a aplicação publicou o evento
 */
public record EventoRelatorioDisponivel(
        String eventoId,
        String tipo,
        Long relatorioId,
        Long pedidoId,
        String solicitacaoId,
        String objectKey,
        String ocorridoEm
) {
}
