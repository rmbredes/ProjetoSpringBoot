package br.com.exemplo.projetospringboot.controller;

import br.com.exemplo.projetospringboot.dto.PedidoDTO;
import br.com.exemplo.projetospringboot.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }


    @PostMapping()
    public ResponseEntity<PedidoDTO> criarPedido(
            @Valid @RequestBody PedidoDTO pedido
    ) {

        PedidoDTO pedidoDTO =
                pedidoService.criarPedido(
                        pedido
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(pedidoDTO);
    }


    /*
     * NOVO
     *
     * GET /pedidos
     */
    @GetMapping
    public ResponseEntity<List<PedidoDTO>> listar() {

        return ResponseEntity.ok(
                pedidoService.listar()
        );
    }


    /*
     * NOVO
     *
     * GET /pedidos/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<PedidoDTO> buscar(
            @PathVariable Long id
    ) {

        return ResponseEntity.ok(
                pedidoService.buscar(id)
        );
    }


    /*
     * NOVO
     *
     * GET /pedidos/cliente/{clienteId}
     */
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<PedidoDTO>> listarPorCliente(
            @PathVariable Long clienteId
    ) {

        return ResponseEntity.ok(
                pedidoService.listaPorCLiente(
                        clienteId
                )
        );
    }


    /*
     * NOVO
     *
     * DELETE /pedidos/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id
    ) {

        pedidoService.excluir(id);

        return ResponseEntity
                .noContent()
                .build();
    }
}
