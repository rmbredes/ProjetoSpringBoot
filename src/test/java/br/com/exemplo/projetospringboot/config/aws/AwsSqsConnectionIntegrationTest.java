package br.com.exemplo.projetospringboot.config.aws;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de integração real entre a aplicação e o Amazon SQS.
 *
 * <p>O teste somente executa quando {@code EXECUTAR_TESTE_AWS=true}.
 * Assim, a suíte normal permanece independente da internet e das
 * credenciais existentes apenas no computador de desenvolvimento.</p>
 */
@SpringBootTest(
        classes = AwsSqsConfig.class,
        properties = {
                "application.aws.sqs.enabled=true",
                "application.aws.sqs.region=sa-east-1",
                "application.aws.sqs.queue-url=https://sqs.sa-east-1.amazonaws.com/033649548808/recomeco-relatorios-pedidos-dev",
                "application.aws.sqs.credentials-profile=projeto-s3",
                "application.aws.sqs.wait-time-seconds=20"
        }
)
@EnabledIfEnvironmentVariable(
        named = "EXECUTAR_TESTE_AWS",
        matches = "true"
)
class AwsSqsConnectionIntegrationTest {

    /** Cliente criado pela mesma configuração usada na aplicação. */
    @Autowired
    private SqsClient sqsClient;

    /** Propriedades externas carregadas pelo Spring. */
    @Autowired
    private AwsSqsProperties properties;

    /**
     * Consulta atributos sem enviar, receber ou excluir mensagens.
     *
     * <p>O teste confirma simultaneamente região, credenciais,
     * permissão IAM e URL da fila.</p>
     */
    @Test
    void deveConsultarAtributosDaFilaPrincipal() {
        GetQueueAttributesRequest request =
                GetQueueAttributesRequest.builder()
                        .queueUrl(properties.queueUrl())
                        .attributeNames(
                                QueueAttributeName.QUEUE_ARN,
                                QueueAttributeName.VISIBILITY_TIMEOUT,
                                QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS,
                                QueueAttributeName.REDRIVE_POLICY
                        )
                        .build();

        GetQueueAttributesResponse response =
                sqsClient.getQueueAttributes(request);

        assertThat(response.attributes()
                .get(QueueAttributeName.QUEUE_ARN))
                .isEqualTo(
                        "arn:aws:sqs:sa-east-1:033649548808:recomeco-relatorios-pedidos-dev"
                );

        assertThat(response.attributes()
                .get(QueueAttributeName.VISIBILITY_TIMEOUT))
                .isEqualTo("120");

        assertThat(response.attributes()
                .get(QueueAttributeName.RECEIVE_MESSAGE_WAIT_TIME_SECONDS))
                .isEqualTo("20");

        assertThat(response.attributes()
                .get(QueueAttributeName.REDRIVE_POLICY))
                .contains("recomeco-relatorios-pedidos-dlq-dev")
                // A AWS representa maxReceiveCount como número no JSON, e não como texto.
                .contains("\"maxReceiveCount\":3");
    }
}
