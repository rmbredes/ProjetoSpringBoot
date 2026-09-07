package br.com.exemplo.lambdaemail.s3;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Testa a leitura do arquivo sem acessar o S3 verdadeiro. */
class LeitorArquivoS3Test {

    @Test
    void deveBuscarObjetoUsandoBucketEObjectKey() {
        S3Client s3Client = mock(S3Client.class);
        byte[] pdfEsperado = "%PDF-arquivo-de-teste".getBytes();

        ResponseBytes<GetObjectResponse> resposta =
                ResponseBytes.fromByteArray(
                        GetObjectResponse.builder().build(),
                        pdfEsperado
                );

        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(resposta);

        LeitorArquivoS3 leitor = new LeitorArquivoS3(
                s3Client,
                "bucket-de-teste"
        );

        byte[] pdfRecebido = leitor.baixar(
                "pedidos/809/relatorios/arquivo.pdf"
        );

        assertArrayEquals(pdfEsperado, pdfRecebido);

        // Captura a requisição para conferir o endereço enviado ao S3Client.
        ArgumentCaptor<GetObjectRequest> captor =
                ArgumentCaptor.forClass(GetObjectRequest.class);

        verify(s3Client).getObjectAsBytes(captor.capture());

        GetObjectRequest requisicao = captor.getValue();
        assertTrue(requisicao.bucket().equals("bucket-de-teste"));
        assertTrue(requisicao.key().equals(
                "pedidos/809/relatorios/arquivo.pdf"
        ));
    }

    @Test
    void deveRecusarNomeDeBucketVazio() {
        S3Client s3Client = mock(S3Client.class);

        assertThrows(
                IllegalStateException.class,
                () -> new LeitorArquivoS3(s3Client, " ")
        );
    }
}
