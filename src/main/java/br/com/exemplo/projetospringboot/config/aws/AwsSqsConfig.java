package br.com.exemplo.projetospringboot.config.aws;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Cria o cliente utilizado para conversar com o Amazon SQS.
 *
 * <p>O cliente é um bean do Spring. Dessa maneira, producer e consumer
 * poderão receber a mesma instância por injeção de dependência.</p>
 *
 * <p>Esta configuração pode ser desligada de forma independente pela
 * propriedade {@code application.aws.sqs.enabled}. Desligar o SQS não
 * interfere no S3, Kafka ou nas demais funcionalidades.</p>
 */
@Configuration
@EnableConfigurationProperties(AwsSqsProperties.class)
@ConditionalOnProperty(
        prefix = "application.aws.sqs",
        name = "enabled",
        havingValue = "true"
)
public class AwsSqsConfig {

    /**
     * Cria o cliente síncrono utilizado nas chamadas ao SQS.
     *
     * <p>No ambiente local, o provedor lê o perfil {@code projeto-s3}.
     * Em uma futura implantação na AWS, o campo do perfil poderá ficar
     * vazio e a cadeia padrão encontrará a role do container.</p>
     *
     * @param properties configurações carregadas do application.yaml
     * @return cliente síncrono do Amazon SQS
     */
    @Bean
    public SqsClient sqsClient(AwsSqsProperties properties) {
        AwsCredentialsProvider credentialsProvider =
                criarProvedorCredenciais(properties);

        Region region = Region.of(properties.region());

        return SqsClient.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();
    }

    /**
     * Escolhe como as credenciais serão obtidas.
     *
     * <p>O método fica privado porque producer e consumer precisam
     * conhecer apenas o SqsClient, e não os detalhes de autenticação.</p>
     */
    private AwsCredentialsProvider criarProvedorCredenciais(
            AwsSqsProperties properties
    ) {
        if (StringUtils.hasText(properties.credentialsProfile())) {
            return ProfileCredentialsProvider.builder()
                    .profileName(properties.credentialsProfile())
                    .build();
        }

        return DefaultCredentialsProvider.builder()
                .build();
    }
}
