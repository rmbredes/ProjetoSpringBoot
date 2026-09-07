package br.com.exemplo.projetospringboot.messaging.producer;

import br.com.exemplo.projetospringboot.config.aws.AwsSqsProperties;
import br.com.exemplo.projetospringboot.dto.GerarRelatorioPedidoMensagem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Producer responsável exclusivamente por enviar solicitações de
 * relatório para o Amazon SQS.
 *
 * <p>Esta classe concentra os detalhes técnicos da AWS. Dessa forma,
 * os serviços que contêm regras de negócio não precisam conhecer
 * {@link SqsClient}, URL de fila ou conversão para JSON.</p>
 *
 * <p>Este producer é independente do {@link PedidoProducer}, que publica
 * eventos no Kafka. Um fluxo pode ser desligado sem alterar o outro.</p>
 */
@Component
@ConditionalOnProperty(
        prefix = "application.aws.sqs",
        name = "enabled",
        havingValue = "true"
)
public class RelatorioPedidoSqsProducer {

    /** Componente utilizado para registrar o envio no log da aplicação. */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(RelatorioPedidoSqsProducer.class);

    /** Cliente do AWS SDK que executa a chamada SendMessage. */
    private final SqsClient sqsClient;

    /** Configurações do SQS carregadas do application.yaml. */
    private final AwsSqsProperties properties;

    /** Componente que transforma o record Java em texto JSON. */
    private final ObjectMapper objectMapper;

    /**
     * Recebe todas as dependências por injeção do Spring.
     *
     * @param sqsClient cliente configurado para acessar o Amazon SQS
     * @param properties configurações que incluem a URL da fila
     * @param objectMapper conversor de objetos Java para JSON
     */
    public RelatorioPedidoSqsProducer(
            SqsClient sqsClient,
            AwsSqsProperties properties,
            ObjectMapper objectMapper
    ) {
        this.sqsClient = sqsClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Envia uma solicitação de relatório para a fila principal.
     *
     * <p>O {@code SqsClient} é síncrono: este método aguarda apenas a AWS
     * confirmar que recebeu a mensagem. A geração do PDF continuará sendo
     * assíncrona, pois será executada posteriormente pelo consumer.</p>
     *
     * @param mensagem dados necessários para processar o relatório
     * @return identificador atribuído pelo SQS à mensagem enviada
     */
    public String enviar(GerarRelatorioPedidoMensagem mensagem) {
        String corpoJson = converterParaJson(mensagem);

        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .messageBody(corpoJson)
                .build();

        SendMessageResponse response = sqsClient.sendMessage(request);

        LOGGER.info(
                "Solicitação de relatório enviada ao SQS: solicitacaoId={}, pedidoId={}, messageId={}",
                mensagem.solicitacaoId(),
                mensagem.pedidoId(),
                response.messageId()
        );

        return response.messageId();
    }

    /**
     * Converte o DTO da mensagem para o formato JSON aceito pela fila.
     *
     * @param mensagem objeto Java que será convertido
     * @return mensagem representada como JSON
     */
    private String converterParaJson(
            GerarRelatorioPedidoMensagem mensagem
    ) {
        try {
            return objectMapper.writeValueAsString(mensagem);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Não foi possível converter a solicitação de relatório para JSON",
                    exception
            );
        }
    }
}
