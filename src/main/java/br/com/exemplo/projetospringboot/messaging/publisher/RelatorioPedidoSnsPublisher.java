package br.com.exemplo.projetospringboot.messaging.publisher;

import br.com.exemplo.projetospringboot.config.aws.AwsSnsProperties;
import br.com.exemplo.projetospringboot.dto.RelatorioPedidoDisponivelEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import tools.jackson.databind.ObjectMapper;

/**
 * Publica no SNS o evento de relatório disponível.
 *
 * <p>Esta classe concentra os detalhes técnicos do SNS. O processador
 * de relatórios precisa conhecer apenas o método {@link #publicar}.</p>
 */
@Component
@ConditionalOnProperty(
        prefix = "application.aws.sns",
        name = "enabled",
        havingValue = "true"
)
public class RelatorioPedidoSnsPublisher {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(RelatorioPedidoSnsPublisher.class);

    private final SnsClient snsClient;
    private final AwsSnsProperties properties;
    private final ObjectMapper objectMapper;

    /** Recebe por injeção o cliente, as configurações e o conversor JSON. */
    public RelatorioPedidoSnsPublisher(
            SnsClient snsClient,
            AwsSnsProperties properties,
            ObjectMapper objectMapper
    ) {
        this.snsClient = snsClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Publica uma notificação no tópico configurado.
     *
     * <p>Uma falha do SNS é registrada, mas não é propagada. O relatório
     * já está disponível no banco e no S3; uma integração opcional não
     * deve transformar essa geração bem-sucedida em falha.</p>
     *
     * @param evento dados do relatório concluído
     */
    public void publicar(RelatorioPedidoDisponivelEvento evento) {
        try {
            String corpoJson = objectMapper.writeValueAsString(evento);

            PublishRequest request = PublishRequest.builder()
                    .topicArn(properties.topicArn())
                    .subject(criarAssunto(evento.pedidoId()))
                    .message(corpoJson)
                    .build();

            PublishResponse response = snsClient.publish(request);

            LOGGER.info(
                    "Evento publicado no SNS: eventoId={}, pedidoId={}, messageId={}",
                    evento.eventoId(),
                    evento.pedidoId(),
                    response.messageId()
            );
        } catch (Exception exception) {
            LOGGER.error(
                    "Não foi possível publicar o evento no SNS: eventoId={}, pedidoId={}",
                    evento.eventoId(),
                    evento.pedidoId(),
                    exception
            );
        }
    }

    /** Cria o assunto simples que será apresentado na assinatura por e-mail. */
    private String criarAssunto(Long pedidoId) {
        return "Relatorio do pedido " + pedidoId + " disponivel";
    }
}
