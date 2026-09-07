package br.com.exemplo.projetospringboot.config.aws;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Configura os componentes utilizados para acessar o Amazon S3.
 *
 * <p>Esta classe cria objetos administrados pelo Spring, chamados beans.
 * Qualquer serviço da aplicação poderá receber esses objetos por
 * injeção de dependência, sem precisar criar uma nova conexão com
 * a AWS a cada operação.</p>
 */
@Configuration
@EnableConfigurationProperties(AwsS3Properties.class)
public class AwsS3Config {

    /**
     * Define como a aplicação obtém credenciais para acessar a AWS.
     *
     * <p>No ambiente local, {@code credentialsProfile} contém
     * {@code projeto-s3}. O SDK procura esse perfil nos arquivos
     * de configuração da AWS CLI.</p>
     *
     * <p>Quando não houver um perfil configurado, usamos a cadeia
     * padrão de credenciais da AWS. Na futura execução no ECS,
     * essa cadeia encontrará automaticamente a role associada
     * ao container.</p>
     *
     * @param properties configurações externas do S3
     * @return provedor responsável por fornecer credenciais temporárias
     */
    @Bean
    public AwsCredentialsProvider awsCredentialsProvider(
            AwsS3Properties properties
    ) {
        /*
         * StringUtils.hasText verifica se o valor não é nulo,
         * não está vazio e não contém somente espaços.
         */
        if (StringUtils.hasText(properties.credentialsProfile())) {
            /*
             * Utilizado durante o desenvolvimento local.
             *
             * O perfil projeto-s3 assume a role com permissões limitadas
             * ao prefixo pedidos/* do nosso bucket.
             */
            return ProfileCredentialsProvider.builder()
                    .profileName(properties.credentialsProfile())
                    .build();
        }

        /*
         * Utilizado quando a aplicação estiver dentro da AWS.
         *
         * O SDK procura credenciais em fontes conhecidas, como:
         * - variáveis de ambiente;
         * - credenciais de containers ECS;
         * - roles associadas a instâncias EC2.
         *
         * Não precisamos colocar access key ou secret key no projeto.
         */
        return DefaultCredentialsProvider.builder()
                .build();
    }

    /**
     * Cria o cliente usado nas operações normais do S3.
     *
     * <p>Esse cliente será usado para enviar, consultar, baixar
     * e excluir objetos.</p>
     *
     * @param properties configurações do S3
     * @param credentialsProvider provedor de credenciais escolhido acima
     * @return cliente síncrono do Amazon S3
     */
    @Bean
    public S3Client s3Client(
            AwsS3Properties properties,
            AwsCredentialsProvider credentialsProvider
    ) {
        /*
         * Region.of transforma o texto "sa-east-1" em um objeto
         * que o AWS SDK reconhece como uma região.
         */
        Region region = Region.of(properties.region());

        return S3Client.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();
    }

    /**
     * Cria o componente responsável por gerar URLs pré-assinadas.
     *
     * <p>Uma URL pré-assinada permite acesso temporário a um objeto
     * privado sem tornar o bucket público.</p>
     *
     * <p>É importante que o presigner use a mesma região e as mesmas
     * credenciais do cliente S3. Caso contrário, a assinatura produzida
     * poderia ser inválida.</p>
     *
     * @param properties configurações do S3
     * @param credentialsProvider provedor de credenciais
     * @return gerador de URLs pré-assinadas do S3
     */
    @Bean
    public S3Presigner s3Presigner(
            AwsS3Properties properties,
            AwsCredentialsProvider credentialsProvider
    ) {
        Region region = Region.of(properties.region());

        return S3Presigner.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();
    }
}