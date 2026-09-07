package br.com.exemplo.projetospringboot.config.aws;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de integração entre a aplicação Spring Boot e o Amazon S3.
 *
 * <p>Diferentemente de um teste unitário, este teste acessa um serviço
 * externo real. Portanto, ele depende de:</p>
 *
 * <ul>
 *     <li>conexão com a internet;</li>
 *     <li>sessão válida criada pelo {@code aws login};</li>
 *     <li>perfil local {@code projeto-s3};</li>
 *     <li>objeto de teste existente no bucket.</li>
 * </ul>
 *
 * <p>O teste somente será executado quando a variável de ambiente
 * {@code EXECUTAR_TESTE_AWS} possuir o valor {@code true}. Isso impede
 * que ele falhe automaticamente na pipeline ou no computador de outro
 * desenvolvedor que não possua credenciais AWS.</p>
 */
/*
 * Carrega somente a configuração necessária para o teste do S3.
 *
 * As propriedades abaixo substituem os valores fictícios do
 * src/test/resources/application.yaml porque este teste acessa
 * intencionalmente o bucket real.
 */
@SpringBootTest(
        classes = AwsS3Config.class,
        properties = {
                "application.aws.s3.bucket-name=recomeco-pedidos-anexos-df33a1eb",
                "application.aws.s3.region=sa-east-1",
                "application.aws.s3.credentials-profile=projeto-s3"
        }
)
@EnabledIfEnvironmentVariable(
        named = "EXECUTAR_TESTE_AWS",
        matches = "true"
)
class AwsS3ConnectionIntegrationTest {

    /**
     * Cliente criado pelo método s3Client da classe AwsS3Config.
     *
     * <p>O teste não cria o cliente manualmente. Ele solicita ao Spring
     * exatamente o mesmo objeto que será utilizado pela aplicação.</p>
     */
    @Autowired
    private S3Client s3Client;

    /**
     * Configurações carregadas da seção application.aws.s3
     * do arquivo application.yaml.
     */
    @Autowired
    private AwsS3Properties properties;

    /**
     * Verifica se a aplicação consegue consultar os metadados
     * do objeto que enviamos anteriormente pela AWS CLI.
     *
     * <p>A operação HEAD não baixa o conteúdo do arquivo. Ela consulta
     * informações como tamanho, tipo e ETag.</p>
     */
    @Test
    void deveConsultarMetadadosDoObjetoNoS3() {
        /*
         * Key completa do objeto dentro do bucket.
         *
         * As barras fazem parte da key. Elas criam a aparência de pastas
         * no console, embora o S3 armazene uma chave textual única.
         */
        String objectKey = "pedidos/1/anexos/teste-cli.txt";

        /*
         * Monta a requisição informando:
         * - qual bucket será consultado;
         * - qual key identifica o objeto.
         */
        HeadObjectRequest request = HeadObjectRequest.builder()
                .bucket(properties.bucketName())
                .key(objectKey)
                .build();

        /*
         * Envia uma requisição HEAD real ao Amazon S3.
         *
         * Se as credenciais, a região, a permissão ou a key estiverem
         * incorretas, o SDK lançará uma exceção e o teste falhará.
         */
        HeadObjectResponse response = s3Client.headObject(request);

        /*
         * Confirma que o arquivo possui conteúdo.
         *
         * O objeto criado no teste anterior contém texto e, portanto,
         * seu tamanho deve ser maior que zero byte.
         */
        assertThat(response.contentLength())
                .isGreaterThan(0L);

        /*
         * Confirma que o S3 devolveu um ETag.
         *
         * O ETag é um identificador calculado pelo S3 para a versão
         * armazenada do objeto. Ele não deve ser tratado sempre como
         * SHA-256 ou MD5, pois seu significado pode variar conforme
         * o modo de upload e a criptografia utilizada.
         */
        assertThat(response.eTag())
                .isNotBlank();
    }
}