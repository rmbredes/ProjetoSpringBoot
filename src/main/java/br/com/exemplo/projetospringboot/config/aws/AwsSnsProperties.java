package br.com.exemplo.projetospringboot.config.aws;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Reúne as configurações usadas para publicar eventos no Amazon SNS.
 *
 * <p>Os valores são carregados da seção {@code application.aws.sns}
 * do arquivo {@code application.yaml}. Assim, região, tópico e perfil
 * podem ser alterados sem modificar as classes Java.</p>
 *
 * @param enabled informa se o addon SNS deve ser criado
 * @param region região AWS em que o tópico existe
 * @param topicArn identificador completo do tópico de relatórios
 * @param credentialsProfile perfil local usado para obter credenciais
 */
@ConfigurationProperties(prefix = "application.aws.sns")
public record AwsSnsProperties(
        boolean enabled,
        String region,
        String topicArn,
        String credentialsProfile
) {
}
