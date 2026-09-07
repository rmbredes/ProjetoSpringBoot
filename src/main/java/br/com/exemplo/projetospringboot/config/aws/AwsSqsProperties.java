package br.com.exemplo.projetospringboot.config.aws;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Reúne as configurações utilizadas para acessar o Amazon SQS.
 *
 * <p>Os valores são carregados da seção {@code application.aws.sqs}
 * existente no arquivo {@code application.yaml}.</p>
 *
 * <p>Manter esses dados fora das classes de negócio permite trocar
 * região, fila ou perfil sem modificar o código Java.</p>
 *
 * @param enabled informa se o addon SQS deve ser iniciado
 * @param region região AWS em que a fila foi criada
 * @param queueUrl endereço completo da fila principal
 * @param credentialsProfile perfil local usado para obter credenciais
 * @param waitTimeSeconds duração máxima do long polling
 * @param consumerEnabled informa se o job consumidor deve consultar a fila
 * @param pollingDelayMs espera entre o término de uma consulta e a próxima
 * @param auditQueueUrl endereço completo da fila de auditoria
 * @param auditConsumerEnabled informa se o consumer de auditoria está ligado
 * @param auditPollingDelayMs espera entre consultas da fila de auditoria
 */
@ConfigurationProperties(prefix = "application.aws.sqs")
public record AwsSqsProperties(
        boolean enabled,
        String region,
        String queueUrl,
        String credentialsProfile,
        int waitTimeSeconds,
        boolean consumerEnabled,
        long pollingDelayMs,
        String auditQueueUrl,
        boolean auditConsumerEnabled,
        long auditPollingDelayMs
) {
}
