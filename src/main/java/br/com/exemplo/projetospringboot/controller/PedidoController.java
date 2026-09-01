package br.com.exemplo.projetospringboot.controller;

import br.com.exemplo.projetospringboot.dto.PedidoDTO;
import br.com.exemplo.projetospringboot.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Expõe as operações HTTP de criação, consulta e exclusão de pedidos.
 */
@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    /**
     * Logger utilizado para acompanhar as operações HTTP de pedidos.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PedidoController.class);

    /** Serviço que concentra as regras de negócio dos pedidos. */
    private final PedidoService pedidoService;

    /**
     * @param pedidoService serviço de pedidos injetado pelo Spring
     */
    public PedidoController(PedidoService pedidoService) {
        // Guarda o serviço que processará as requisições deste controlador.
        this.pedidoService = pedidoService;
    }

    /**
     * Cria um pedido e o respectivo evento na Outbox.
     *
     * @param pedido dados validados do novo pedido
     * @return resposta 201 com o pedido criado
     */
    @PostMapping()
    public ResponseEntity<PedidoDTO> criarPedido(
            @Valid @RequestBody PedidoDTO pedido
    ) {



        // Delega ao serviço a criação transacional do pedido.
        PedidoDTO pedidoDTO =
                pedidoService.criarPedido(
                        pedido
                );
        LOGGER.info(
                "Pedido criado: pedidoId={}",
                pedidoDTO.id()
        );

        // Informa ao cliente que o recurso foi criado com sucesso.
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(pedidoDTO);
    }


    /**
     * Lista todos os pedidos cadastrados.
     *
     * @return resposta 200 com a lista de pedidos
     */
    @GetMapping
    public ResponseEntity<List<PedidoDTO>> listar() {

        LOGGER.info("Listagem de pedidos solicitada");

        // Obtém a lista no serviço e a inclui na resposta HTTP.
        return ResponseEntity.ok(
                pedidoService.listar()
        );
    }


    /**
     * Busca um pedido pelo identificador.
     *
     * @param id identificador do pedido
     * @return resposta 200 com o pedido encontrado
     */
    @GetMapping("/{id}")
    public ResponseEntity<PedidoDTO> buscar(
            @PathVariable Long id
    ) {

        // Delega a busca ao serviço e devolve seu resultado.
        return ResponseEntity.ok(
                pedidoService.buscar(id)
        );
    }


    /**
     * Lista os pedidos pertencentes a um cliente.
     *
     * @param clienteId identificador do cliente
     * @return resposta 200 com os pedidos encontrados
     */
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<PedidoDTO>> listarPorCliente(
            @PathVariable Long clienteId
    ) {

        // Filtra os pedidos por meio da consulta implementada no serviço.
        return ResponseEntity.ok(
                pedidoService.listaPorCLiente(
                        clienteId
                )
        );
    }


    /**
     * Exclui um pedido pelo identificador.
     *
     * @param id identificador do pedido
     * @return resposta 204 sem corpo
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id
    ) {

        // Solicita ao serviço a exclusão do pedido.
        pedidoService.excluir(id);

        // Responde sem conteúdo porque o recurso deixou de existir.
        return ResponseEntity
                .noContent()
                .build();
    }
}
