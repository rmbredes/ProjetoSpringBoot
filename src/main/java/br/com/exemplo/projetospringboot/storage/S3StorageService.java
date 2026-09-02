package br.com.exemplo.projetospringboot.storage;

import br.com.exemplo.projetospringboot.config.aws.AwsS3Properties;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.io.InputStream;

/**
 * Encapsula as operações de armazenamento realizadas no Amazon S3.
 *
 * <p>Esta classe conhece detalhes técnicos do S3, como bucket, key,
 * PutObjectRequest e RequestBody. As outras camadas da aplicação não
 * precisarão conhecer esses detalhes.</p>
 *
 * <p>Nesta primeira etapa, implementamos somente o envio de objetos.
 * Depois adicionaremos consulta, download, geração de URL temporária
 * e exclusão.</p>
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
            AwsS3Properties properties
    ) {
        this.s3Client = s3Client;
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
}