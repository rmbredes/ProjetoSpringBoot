package br.com.exemplo.projetospringboot.service;

import br.com.exemplo.projetospringboot.dto.GerarRelatorioPedidoMensagem;
import br.com.exemplo.projetospringboot.dto.RelatorioPedidoDisponivelEvento;
import br.com.exemplo.projetospringboot.entity.RelatorioPedido;
import br.com.exemplo.projetospringboot.enums.StatusRelatorioPedido;
import br.com.exemplo.projetospringboot.messaging.publisher.RelatorioPedidoSnsPublisher;
import br.com.exemplo.projetospringboot.report.GeradorRelatorioPedidoPdf;
import br.com.exemplo.projetospringboot.repository.RelatorioPedidoRepository;
import br.com.exemplo.projetospringboot.storage.S3StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * Processa uma solicitação recebida do SQS e produz seu arquivo.
 *
 * <p>Esta classe coordena banco, gerador de PDF e armazenamento. Os
 * detalhes de cada tecnologia continuam encapsulados em suas classes.</p>
 */
@Service
@ConditionalOnProperty(
        prefix = "application.aws.sqs",
        name = "enabled",
        havingValue = "true"
)
public class ProcessadorRelatorioPedidoService {

    /** Tipo MIME utilizado no upload do relatório. */
    private static final String CONTENT_TYPE_PDF = "application/pdf";

    /** Logger usado para registrar a conclusão do processamento. */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ProcessadorRelatorioPedidoService.class);

    private final RelatorioPedidoRepository relatorioPedidoRepository;
    private final GeradorRelatorioPedidoPdf geradorPdf;
    private final S3StorageService storageService;

    /**
     * Publicador opcional do SNS.
     *
     * <p>Quando o addon SNS está desligado, o Spring injeta um Optional
     * vazio e todo o restante do processamento continua normalmente.</p>
     */
    private final Optional<RelatorioPedidoSnsPublisher> snsPublisher;

    /**
     * @param relatorioPedidoRepository repositório das solicitações
     * @param geradorPdf componente que cria o documento em memória
     * @param storageService componente que realiza o upload para o S3
     * @param snsPublisher publicador presente somente quando o SNS está ligado
     */
    public ProcessadorRelatorioPedidoService(
            RelatorioPedidoRepository relatorioPedidoRepository,
            GeradorRelatorioPedidoPdf geradorPdf,
            S3StorageService storageService,
            Optional<RelatorioPedidoSnsPublisher> snsPublisher
    ) {
        this.relatorioPedidoRepository = relatorioPedidoRepository;
        this.geradorPdf = geradorPdf;
        this.storageService = storageService;
        this.snsPublisher = snsPublisher;
    }

    /**
     * Gera e armazena o relatório descrito pela mensagem.
     *
     * <p>Se o mesmo conteúdo for entregue novamente depois de uma
     * conclusão bem-sucedida, o método simplesmente retorna. Essa é uma
     * proteção básica contra entregas duplicadas do SQS.</p>
     *
     * @param mensagem conteúdo recebido da fila
     */
    public void processar(GerarRelatorioPedidoMensagem mensagem) {
        RelatorioPedido relatorio = relatorioPedidoRepository
                .findBySolicitacaoId(mensagem.solicitacaoId())
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Solicitação de relatório não encontrada"
                        )
                );

        validarMensagem(relatorio, mensagem);

        if (relatorio.getStatus()
                == StatusRelatorioPedido.DISPONIVEL) {
            LOGGER.info(
                    "Relatório já estava disponível: solicitacaoId={}",
                    mensagem.solicitacaoId()
            );
            return;
        }

        if (relatorio.getStatus()
                != StatusRelatorioPedido.ENFILEIRADO) {
            throw new IllegalStateException(
                    "A solicitação não está pronta para processamento: status="
                            + relatorio.getStatus()
            );
        }

        byte[] pdf = geradorPdf.gerar(relatorio);
        String checksumSha256 = calcularSha256(pdf);
        String nomeArquivo = criarNomeArquivo(mensagem.pedidoId());
        String objectKey = criarObjectKey(
                mensagem.pedidoId(),
                mensagem.solicitacaoId()
        );

        String eTag = storageService.upload(
                objectKey,
                new ByteArrayInputStream(pdf),
                pdf.length,
                CONTENT_TYPE_PDF
        );

        relatorio.marcarComoDisponivel(
                objectKey,
                nomeArquivo,
                pdf.length,
                checksumSha256,
                eTag
        );

        relatorioPedidoRepository.save(relatorio);

        /*
         * Somente depois de banco e S3 confirmarem a conclusão informamos
         * ao mundo externo que o relatório está disponível. Se o addon SNS
         * estiver desligado, o Optional estará vazio e nada será publicado.
         */
        publicarEventoRelatorioDisponivel(relatorio);

        LOGGER.info(
                "Relatório concluído: relatorioId={}, pedidoId={}, objectKey={}, tamanhoBytes={}",
                relatorio.getId(),
                mensagem.pedidoId(),
                objectKey,
                pdf.length
        );
    }

    /** Publica o fato concluído quando o addon SNS estiver habilitado. */
    private void publicarEventoRelatorioDisponivel(
            RelatorioPedido relatorio
    ) {
        snsPublisher.ifPresent(publicador -> {
            RelatorioPedidoDisponivelEvento evento =
                    new RelatorioPedidoDisponivelEvento(
                            UUID.randomUUID(),
                            "RELATORIO_PEDIDO_DISPONIVEL",
                            relatorio.getId(),
                            relatorio.getPedido().getId(),
                            relatorio.getSolicitacaoId(),
                            relatorio.getObjectKey(),
                            Instant.now()
                    );

            publicador.publicar(evento);
        });
    }

    /**
     * Marca no banco uma falha que esgotou as tentativas da fila.
     *
     * @param solicitacaoId identificador da solicitação
     * @param mensagemErro descrição resumida da falha
     */
    public void registrarFalhaDefinitiva(
            UUID solicitacaoId,
            String mensagemErro
    ) {
        relatorioPedidoRepository
                .findBySolicitacaoId(solicitacaoId)
                .ifPresent(relatorio -> {
                    if (relatorio.getStatus()
                            != StatusRelatorioPedido.DISPONIVEL) {
                        relatorio.marcarComoFalha(mensagemErro);
                        relatorioPedidoRepository.save(relatorio);
                    }
                });
    }

    /** Confirma que os identificadores da mensagem pertencem ao registro. */
    private void validarMensagem(
            RelatorioPedido relatorio,
            GerarRelatorioPedidoMensagem mensagem
    ) {
        if (!relatorio.getId().equals(mensagem.relatorioId())
                || !relatorio.getPedido().getId().equals(
                        mensagem.pedidoId()
                )) {
            throw new IllegalArgumentException(
                    "A mensagem não corresponde à solicitação armazenada"
            );
        }
    }

    /** Cria uma key previsível e exclusiva dentro da área de relatórios. */
    private String criarObjectKey(
            Long pedidoId,
            UUID solicitacaoId
    ) {
        return "pedidos/"
                + pedidoId
                + "/relatorios/"
                + solicitacaoId
                + ".pdf";
    }

    /** Cria o nome amigável que será sugerido no futuro download. */
    private String criarNomeArquivo(Long pedidoId) {
        return "relatorio-pedido-" + pedidoId + ".pdf";
    }

    /** Calcula o hash SHA-256 dos mesmos bytes enviados ao S3. */
    private String calcularSha256(byte[] conteudo) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            return HexFormat
                    .of()
                    .formatHex(digest.digest(conteudo));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 não está disponível",
                    exception
            );
        }
    }
}
