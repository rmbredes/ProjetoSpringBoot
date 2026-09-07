package br.com.exemplo.projetospringboot.storage;

import br.com.exemplo.projetospringboot.config.aws.AwsS3Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Testes unitários do serviço responsável pelo armazenamento no S3.
 *
 * <p>Estes testes não acessam a AWS. O S3Client é substituído por
 * um mock controlado pelo Mockito.</p>
 *
 * <p>O objetivo é verificar se o serviço:</p>
 *
 * <ul>
 *     <li>utiliza o bucket configurado;</li>
 *     <li>envia a key correta;</li>
 *     <li>informa o tipo do conteúdo;</li>
 *     <li>chama a operação putObject;</li>
 *     <li>devolve o ETag retornado pelo S3.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    /**
     * Simulação do cliente S3.
     *
     * <p>Nenhuma requisição de rede será enviada por este objeto.</p>
     */
    @Mock
    private S3Client s3Client;

    /** Simulação do componente que assina URLs de download. */
    @Mock
    private S3Presigner s3Presigner;

    /**
     * Serviço que será testado.
     */
    private S3StorageService storageService;

    /**
     * Cria uma nova instância do serviço antes de cada teste.
     */
    @BeforeEach
    void configurarTeste() {
        /*
         * Configuração fictícia utilizada somente pelo teste unitário.
         *
         * Região e perfil não são usados diretamente pelo
         * S3StorageService, mas fazem parte do record.
         */
        AwsS3Properties properties = new AwsS3Properties(
                "bucket-teste",
                "sa-east-1",
                ""
        );

        /*
         * Criamos o serviço manualmente para deixar explícitas
         * as dependências utilizadas pelo teste.
         */
        storageService = new S3StorageService(
                s3Client,
                s3Presigner,
                properties
        );
    }

    /**
     * Verifica o envio de um objeto e o retorno de seu ETag.
     */
    @Test
    void deveEnviarObjetoParaOBucketConfigurado() {
        /*
         * Conteúdo pequeno e previsível usado no teste.
         */
        byte[] conteudo = "arquivo de teste"
                .getBytes(StandardCharsets.UTF_8);

        /*
         * Transforma os bytes em um InputStream, como aconteceria
         * com o conteúdo recebido pela aplicação.
         */
        InputStream inputStream = new ByteArrayInputStream(conteudo);

        String objectKey = "pedidos/1/anexos/arquivo.txt";
        String contentType = "text/plain";

        /*
         * Simula a resposta que o S3 devolveria após um PUT bem-sucedido.
         */
        PutObjectResponse respostaSimulada = PutObjectResponse.builder()
                .eTag("\"etag-teste\"")
                .build();

        /*
         * Programa o mock:
         *
         * quando putObject receber uma requisição e um corpo,
         * deve devolver a resposta simulada, sem acessar a AWS.
         */
        when(s3Client.putObject(
                any(PutObjectRequest.class),
                any(RequestBody.class)
        )).thenReturn(respostaSimulada);

        /*
         * Executa o método que realmente estamos testando.
         */
        String eTag = storageService.upload(
                objectKey,
                inputStream,
                conteudo.length,
                contentType
        );

        /*
         * Captura a PutObjectRequest entregue ao S3Client.
         *
         * Isso permite examinar os dados que o serviço enviaria à AWS.
         */
        ArgumentCaptor<PutObjectRequest> requestCaptor =
                ArgumentCaptor.forClass(PutObjectRequest.class);

        /*
         * Confirma que putObject foi chamado e captura seu
         * primeiro argumento.
         */
        verify(s3Client).putObject(
                requestCaptor.capture(),
                any(RequestBody.class)
        );

        PutObjectRequest requestEnviada = requestCaptor.getValue();

        /*
         * Confirma que o bucket veio das configurações.
         */
        assertThat(requestEnviada.bucket())
                .isEqualTo("bucket-teste");

        /*
         * Confirma que a key recebida pelo serviço foi preservada.
         */
        assertThat(requestEnviada.key())
                .isEqualTo(objectKey);

        /*
         * Confirma que o tipo do arquivo foi enviado como metadado.
         */
        assertThat(requestEnviada.contentType())
                .isEqualTo(contentType);

        /*
         * Confirma que o serviço devolveu o ETag da resposta do S3.
         */
        assertThat(eTag)
                .isEqualTo("\"etag-teste\"");
    }

    /**
     * Confirma que a URL é assinada para a key, o nome e a duração
     * informados pela camada de negócio.
     */
    @Test
    void deveGerarUrlTemporariaParaDownload() throws Exception {
        PresignedGetObjectRequest respostaAssinada =
                org.mockito.Mockito.mock(
                        PresignedGetObjectRequest.class
                );

        when(respostaAssinada.url())
                .thenReturn(
                        URI.create(
                                "https://bucket-teste.s3.amazonaws.com/objeto?assinatura=teste"
                        ).toURL()
                );

        when(s3Presigner.presignGetObject(
                any(GetObjectPresignRequest.class)
        )).thenReturn(respostaAssinada);

        URI resultado = storageService.generateDownloadUrl(
                "pedidos/1/anexos/arquivo",
                "nota fiscal.pdf",
                Duration.ofMinutes(5)
        );

        ArgumentCaptor<GetObjectPresignRequest> captor =
                ArgumentCaptor.forClass(
                        GetObjectPresignRequest.class
                );

        verify(s3Presigner).presignGetObject(
                captor.capture()
        );

        GetObjectPresignRequest requisicao =
                captor.getValue();

        GetObjectRequest getObjectRequest =
                requisicao.getObjectRequest();

        assertThat(requisicao.signatureDuration())
                .isEqualTo(Duration.ofMinutes(5));

        assertThat(getObjectRequest.bucket())
                .isEqualTo("bucket-teste");

        assertThat(getObjectRequest.key())
                .isEqualTo("pedidos/1/anexos/arquivo");

        assertThat(getObjectRequest.responseContentDisposition())
                .isEqualTo(
                        "attachment; filename*=UTF-8''nota%20fiscal.pdf"
                );

        assertThat(resultado.toString())
                .contains("assinatura=teste");
    }

    /**
     * Confirma que a exclusão utiliza o bucket configurado e a key
     * recebida do serviço de anexos.
     */
    @Test
    void deveExcluirObjetoDoBucketConfigurado() {
        when(s3Client.deleteObject(
                any(DeleteObjectRequest.class)
        )).thenReturn(
                DeleteObjectResponse.builder().build()
        );

        storageService.delete(
                "pedidos/1/anexos/arquivo"
        );

        ArgumentCaptor<DeleteObjectRequest> captor =
                ArgumentCaptor.forClass(
                        DeleteObjectRequest.class
                );

        verify(s3Client).deleteObject(
                captor.capture()
        );

        assertThat(captor.getValue().bucket())
                .isEqualTo("bucket-teste");

        assertThat(captor.getValue().key())
                .isEqualTo("pedidos/1/anexos/arquivo");
    }
}
