package br.com.exemplo.lambdaemail.s3;

import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * Busca no Amazon S3 o PDF que será anexado ao e-mail.
 *
 * <p>A classe precisa de duas informações para encontrar um objeto:</p>
 *
 * <pre>
 * nome do bucket + objectKey = objeto armazenado no S3
 * </pre>
 */
public class LeitorArquivoS3 {

    /** Nome da variável de ambiente configurada na função Lambda. */
    public static final String VARIAVEL_BUCKET = "BUCKET_RELATORIOS";

    private final S3Client s3Client;
    private final String bucket;

    /**
     * Construtor usado na AWS.
     *
     * <p>O {@link S3Client#create()} encontra automaticamente a região e as
     * credenciais temporárias fornecidas pela execution role da Lambda.</p>
     */
    public LeitorArquivoS3() {
        this(
                S3Client.create(),
                System.getenv(VARIAVEL_BUCKET)
        );
    }

    /**
     * Construtor usado pelos testes para fornecer um cliente S3 controlado.
     */
    LeitorArquivoS3(
            S3Client s3Client,
            String bucket
    ) {
        this.s3Client = s3Client;
        this.bucket = validarBucket(bucket);
    }

    /**
     * Baixa o conteúdo completo do objeto e o devolve como um vetor de bytes.
     * Um PDF é um arquivo binário; por isso não deve ser tratado como String.
     *
     * @param objectKey endereço completo do objeto dentro do bucket
     * @return bytes que formam o arquivo PDF
     */
    public byte[] baixar(String objectKey) {
        GetObjectRequest requisicao = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        ResponseBytes<GetObjectResponse> resposta =
                s3Client.getObjectAsBytes(requisicao);

        return resposta.asByteArray();
    }

    /** Impede a execução com uma configuração vazia ou ausente. */
    private String validarBucket(String nomeBucket) {
        if (nomeBucket == null || nomeBucket.isBlank()) {
            throw new IllegalStateException(
                    "A variável de ambiente "
                            + VARIAVEL_BUCKET
                            + " não foi configurada"
            );
        }

        return nomeBucket;
    }
}
