package br.com.exemplo.projetospringboot.config.aws;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Representa as configurações utilizadas pela integração com o Amazon S3.
 *
 * <p>Os valores desta classe são carregados da seção
 * {@code application.aws.s3} do arquivo {@code application.yaml}.</p>
 *
 * <p>Usar uma classe específica para configurações oferece algumas vantagens:</p>
 *
 * <ul>
 *     <li>evita repetir valores em diferentes classes;</li>
 *     <li>mantém o código Java separado das configurações de ambiente;</li>
 *     <li>facilita trocar bucket, região ou perfil sem alterar a lógica;</li>
 *     <li>facilita utilizar configurações diferentes localmente e na AWS.</li>
 * </ul>
 *
 * @param bucketName nome do bucket que armazena os anexos
 * @param region região AWS em que o bucket foi criado
 * @param credentialsProfile perfil local utilizado para obter as credenciais
 */
@ConfigurationProperties(prefix = "application.aws.s3")
public record AwsS3Properties(
        String bucketName,
        String region,
        String credentialsProfile
) {
}