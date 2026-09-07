package br.com.exemplo.projetospringboot.controller;

import br.com.exemplo.projetospringboot.dto.RelatorioPedidoDTO;
import br.com.exemplo.projetospringboot.dto.UrlDownloadRelatorioDTO;
import br.com.exemplo.projetospringboot.enums.StatusRelatorioPedido;
import br.com.exemplo.projetospringboot.service.RelatorioPedidoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expõe as operações HTTP relacionadas aos relatórios dos pedidos.
 *
 * <p>O controller conhece apenas HTTP e o serviço de negócio.
 * Ele não conhece SqsClient, URL de fila ou detalhes do AWS SDK.</p>
 */
@RestController
@RequestMapping("/pedidos/{pedidoId}/relatorios")
@ConditionalOnProperty(
        prefix = "application.aws.sqs",
        name = "enabled",
        havingValue = "true"
)
public class RelatorioPedidoController {

    /** Logger utilizado para registrar a entrada da solicitação HTTP. */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(RelatorioPedidoController.class);

    /** Serviço que coordena banco de dados e publicação no SQS. */
    private final RelatorioPedidoService relatorioPedidoService;

    /**
     * @param relatorioPedidoService serviço responsável pelas solicitações
     */
    public RelatorioPedidoController(
            RelatorioPedidoService relatorioPedidoService
    ) {
        this.relatorioPedidoService = relatorioPedidoService;
    }

    /**
     * Solicita a geração assíncrona de um relatório para o pedido.
     *
     * <p>O endpoint não recebe body. O identificador existente na URL
     * já informa qual pedido deverá aparecer no relatório.</p>
     *
     * <p>HTTP 202 (Accepted) significa que a solicitação foi aceita
     * e enfileirada, mas seu processamento ainda não terminou.</p>
     *
     * @param pedidoId identificador do pedido
     * @return resposta 202 com os dados da solicitação enfileirada
     */
    @PostMapping
    public ResponseEntity<RelatorioPedidoDTO> solicitar(
            @PathVariable Long pedidoId
    ) {
        LOGGER.info(
                "Geração de relatório solicitada: pedidoId={}",
                pedidoId
        );

        RelatorioPedidoDTO relatorio =
                relatorioPedidoService.solicitar(pedidoId);

        /*
         * Uma nova solicitação volta ENFILEIRADA e recebe HTTP 202.
         * Se o serviço reutilizar um relatório já DISPONIVEL, HTTP 200
         * informa que o recurso pedido já está pronto.
         */
        HttpStatus statusResposta =
                relatorio.status() == StatusRelatorioPedido.DISPONIVEL
                        ? HttpStatus.OK
                        : HttpStatus.ACCEPTED;

        return ResponseEntity
                .status(statusResposta)
                .body(relatorio);
    }

    /**
     * Lista todas as solicitações de relatório pertencentes ao pedido.
     *
     * @param pedidoId identificador do pedido
     * @return resposta 200 com o histórico de relatórios
     */
    @GetMapping
    public ResponseEntity<List<RelatorioPedidoDTO>> listar(
            @PathVariable Long pedidoId
    ) {
        return ResponseEntity.ok(
                relatorioPedidoService.listar(pedidoId)
        );
    }

    /**
     * Gera uma URL válida por cinco minutos para download direto do S3.
     *
     * @param pedidoId identificador do pedido proprietário
     * @param relatorioId identificador interno do relatório
     * @return resposta 200 com URL e instante de expiração
     */
    @GetMapping("/{relatorioId}/download")
    public ResponseEntity<UrlDownloadRelatorioDTO> gerarUrlDownload(
            @PathVariable Long pedidoId,
            @PathVariable Long relatorioId
    ) {
        return ResponseEntity.ok(
                relatorioPedidoService.gerarUrlDownload(
                        pedidoId,
                        relatorioId
                )
        );
    }
}
