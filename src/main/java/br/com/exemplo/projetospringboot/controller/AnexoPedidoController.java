package br.com.exemplo.projetospringboot.controller;

import br.com.exemplo.projetospringboot.dto.AnexoPedidoDTO;
import br.com.exemplo.projetospringboot.service.AnexoPedidoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
}