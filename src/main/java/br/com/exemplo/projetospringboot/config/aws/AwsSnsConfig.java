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
import software.amazon.awssdk.services.sns.SnsClient;

/**
 * Cria o cliente utilizado para publicar mensagens no Amazon SNS.
 *
 * <p>A configuração é carregada somente quando
 * {@code application.aws.sns.enabled=true}. Portanto, desligar o SNS
 * não interfere no SQS, S3, Kafka ou nos endpoints da aplicação.</p>
 */
@Configuration
@EnableConfigurationProperties(AwsSnsProperties.class)
@ConditionalOnProperty(
        prefix = "application.aws.sns",
        name = "enabled",
        havingValue = "true"
)
public class AwsSnsConfig {

    /**
     * Cria uma única instância do cliente síncrono do SNS.
     *
     * <p>Síncrono significa que a chamada aguarda a AWS confirmar
     * a publicação no tópico. A distribuição aos assinantes continua
     * sendo executada de forma assíncrona pelo próprio SNS.</p>
     *
     * @param properties configurações carregadas do application.yaml
     * @return cliente configurado para a região do tópico
     */
    @Bean
    public SnsClient snsClient(AwsSnsProperties properties) {
        AwsCredentialsProvider credentialsProvider =
                criarProvedorCredenciais(properties);

        return SnsClient.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(credentialsProvider)
                .build();
    }

    /** Escolhe entre o perfil local e a cadeia padrão de credenciais. */
    private AwsCredentialsProvider criarProvedorCredenciais(
            AwsSnsProperties properties
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
