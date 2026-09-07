package br.com.exemplo.projetospringboot.controller;

import br.com.exemplo.projetospringboot.dto.AnexoPedidoDTO;
import br.com.exemplo.projetospringboot.dto.UrlDownloadAnexoDTO;
import br.com.exemplo.projetospringboot.service.AnexoPedidoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Expõe as operações HTTP relacionadas aos anexos dos pedidos.
 */
@RestController
@RequestMapping("/pedidos/{pedidoId}/anexos")
public class AnexoPedidoController {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AnexoPedidoController.class);

    private final AnexoPedidoService anexoPedidoService;

    /**
     * @param anexoPedidoService serviço que processa os anexos
     */
    public AnexoPedidoController(
            AnexoPedidoService anexoPedidoService
    ) {
        this.anexoPedidoService = anexoPedidoService;
    }

    /**
     * Recebe um arquivo multipart e o associa a um pedido.
     *
     * <p>O nome esperado para o campo multipart é {@code arquivo}.</p>
     *
     * @param pedidoId pedido que receberá o anexo
     * @param arquivo conteúdo enviado pelo cliente
     * @return resposta 201 com os metadados do anexo
     */
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<AnexoPedidoDTO> enviar(
            @PathVariable Long pedidoId,
            @RequestPart("arquivo") MultipartFile arquivo
    ) {
        LOGGER.info(
                "Upload de anexo solicitado: pedidoId={}, nome={}, tamanhoBytes={}",
                pedidoId,
                arquivo.getOriginalFilename(),
                arquivo.getSize()
        );

        AnexoPedidoDTO anexo =
                anexoPedidoService.enviar(
                        pedidoId,
                        arquivo
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(anexo);
    }

    /**
     * Lista os anexos que continuam disponíveis para o pedido.
     *
     * @param pedidoId identificador do pedido
     * @return resposta 200 com a lista de metadados
     */
    @GetMapping
    public ResponseEntity<List<AnexoPedidoDTO>> listar(
            @PathVariable Long pedidoId
    ) {
        return ResponseEntity.ok(
                anexoPedidoService.listar(pedidoId)
        );
    }

    /**
     * Gera uma URL temporária para download direto do S3.
     *
     * @param pedidoId identificador do pedido proprietário
     * @param anexoId identificador do anexo
     * @return resposta 200 com URL e expiração
     */
    @GetMapping("/{anexoId}/download")
    public ResponseEntity<UrlDownloadAnexoDTO> gerarUrlDownload(
            @PathVariable Long pedidoId,
            @PathVariable Long anexoId
    ) {
        return ResponseEntity.ok(
                anexoPedidoService.gerarUrlDownload(
                        pedidoId,
                        anexoId
                )
        );
    }

    /**
     * Remove o objeto do S3 e mantém seu histórico no banco.
     *
     * @param pedidoId identificador do pedido proprietário
     * @param anexoId identificador do anexo
     * @return resposta 204 sem corpo
     */
    @DeleteMapping("/{anexoId}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long pedidoId,
            @PathVariable Long anexoId
    ) {
        anexoPedidoService.excluir(
                pedidoId,
                anexoId
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}
