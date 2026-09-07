package br.com.exemplo.projetospringboot.storage;

import br.com.exemplo.projetospringboot.config.aws.AwsS3Properties;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Encapsula as operações de armazenamento realizadas no Amazon S3.
 *
 * <p>Esta classe conhece detalhes técnicos do S3, como bucket, key,
 * PutObjectRequest e RequestBody. As outras camadas da aplicação não
 * precisarão conhecer esses detalhes.</p>
 *
 * <p>A classe implementa o envio, a geração de URL temporária para
 * download e a exclusão de objetos. Nenhuma dessas operações torna
 * o bucket público.</p>
 */
@Service
public class S3StorageService {

    /**
     * Cliente fornecido pelo AwsS3Config.
     *
     * <p>Ele realiza as chamadas HTTPS para a API do Amazon S3.</p>
     */
    private final S3Client s3Client;

    /**
     * Componente que cria URLs assinadas sem alterar a privacidade
     * configurada no bucket.
     */
    private final S3Presigner s3Presigner;

    /**
     * Configurações carregadas do application.yaml.
     *
     * <p>Por meio deste objeto obtemos, entre outros valores,
     * o nome do bucket utilizado pela aplicação.</p>
     */
    private final AwsS3Properties properties;

    /**
     * Construtor utilizado pelo Spring para injetar as dependências.
     *
     * <p>Preferimos injeção pelo construtor porque:</p>
     *
     * <ul>
     *     <li>as dependências ficam explícitas;</li>
     *     <li>os campos podem ser imutáveis;</li>
     *     <li>a classe fica mais fácil de testar;</li>
     *     <li>não precisamos usar campos anotados com Autowired.</li>
     * </ul>
     *
     * @param s3Client cliente utilizado para acessar o S3
     * @param properties configurações da integração com o S3
     */
    public S3StorageService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            AwsS3Properties properties
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    /**
     * Envia um objeto para o bucket configurado.
     *
     * <p>O método recebe um InputStream para não precisar carregar
     * obrigatoriamente todo o arquivo na memória. Isso será importante
     * quando recebermos o conteúdo enviado por uma requisição HTTP.</p>
     *
     * <p>O stream é lido durante a chamada síncrona ao S3, mas não é
     * fechado por este método. Quem abriu o stream continua responsável
     * por fechá-lo.</p>
     *
     * @param objectKey key completa que identificará o objeto no bucket
     * @param inputStream fluxo contendo os bytes do arquivo
     * @param contentLength tamanho exato do arquivo em bytes
     * @param contentType tipo do conteúdo, como application/pdf
     * @return ETag devolvido pelo S3 após o armazenamento
     */
    public String upload(
            String objectKey,
            InputStream inputStream,
            long contentLength,
            String contentType
    ) {
        /*
         * PutObjectRequest contém os metadados da operação.
         *
         * O conteúdo do arquivo não fica dentro desta requisição.
         * Ele será fornecido separadamente pelo RequestBody.
         */
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucketName())
                .key(objectKey)
                .contentType(contentType)
                .build();

        /*
         * RequestBody representa o corpo enviado ao S3.
         *
         * O tamanho precisa ser exato. Um valor incorreto pode causar
         * falha no envio ou deixar a operação esperando mais bytes.
         */
        RequestBody requestBody = RequestBody.fromInputStream(
                inputStream,
                contentLength
        );

        /*
         * putObject corresponde à operação PUT que testamos pela CLI.
         *
         * Como S3Client é síncrono, este método aguarda a resposta do S3.
         * Em caso de falha, o AWS SDK lança uma exceção.
         */
        PutObjectResponse response = s3Client.putObject(
                request,
                requestBody
        );

        /*
         * Retornamos o ETag para que a camada superior possa registrar
         * uma referência da versão armazenada do objeto.
         *
         * O ETag não deve ser considerado sempre um hash MD5.
         */
        return response.eTag();
    }

    /**
     * Cria uma URL temporária para baixar um objeto privado.
     *
     * <p>O nome original é incluído em Content-Disposition para que
     * o navegador sugira um nome compreensível ao salvar o arquivo,
     * em vez de utilizar o UUID existente no final da key.</p>
     *
     * @param objectKey key do objeto no bucket
     * @param nomeOriginal nome sugerido durante o download
     * @param duracao tempo durante o qual a URL será aceita
     * @return URI assinada que aponta diretamente para o S3
     */
    public URI generateDownloadUrl(
            String objectKey,
            String nomeOriginal,
            Duration duracao
    ) {
        /*
         * Codifica espaços, acentos e caracteres especiais segundo
         * o formato aceito no parâmetro filename* do cabeçalho HTTP.
         */
        String nomeCodificado = URLEncoder
                .encode(
                        nomeOriginal,
                        StandardCharsets.UTF_8
                )
                .replace("+", "%20");

        GetObjectRequest getObjectRequest =
                GetObjectRequest.builder()
                        .bucket(properties.bucketName())
                        .key(objectKey)
                        .responseContentDisposition(
                                "attachment; filename*=UTF-8''"
                                        + nomeCodificado
                        )
                        .build();

        /*
         * A duração passa a fazer parte da assinatura. Depois desse
         * período, o próprio S3 recusará a mesma URL.
         */
        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(duracao)
                        .getObjectRequest(getObjectRequest)
                        .build();

        PresignedGetObjectRequest presignedRequest =
                s3Presigner.presignGetObject(presignRequest);

        return URI.create(
                presignedRequest.url().toString()
        );
    }

    /**
     * Remove um objeto do bucket.
     *
     * <p>A operação DELETE do S3 é idempotente: solicitar novamente
     * a remoção da mesma key não recria o objeto nem produz conteúdo.</p>
     *
     * @param objectKey key do objeto que será removido
     */
    public void delete(String objectKey) {
        DeleteObjectRequest request =
                DeleteObjectRequest.builder()
                        .bucket(properties.bucketName())
                        .key(objectKey)
                        .build();

        s3Client.deleteObject(request);
    }
}
