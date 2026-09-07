package br.com.exemplo.projetospringboot.controller;

import br.com.exemplo.projetospringboot.dto.AuditoriaRelatorioDTO;
import br.com.exemplo.projetospringboot.service.AuditoriaRelatorioService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Expõe a consulta REST dos eventos gravados pela auditoria. */
@RestController
@RequestMapping("/pedidos/{pedidoId}/auditorias-relatorios")
@ConditionalOnProperty(
        prefix = "application.aws.sqs",
        name = "enabled",
        havingValue = "true"
)
public class AuditoriaRelatorioController {

    private final AuditoriaRelatorioService service;

    public AuditoriaRelatorioController(
            AuditoriaRelatorioService service
    ) {
        this.service = service;
    }

    /**
     * Lista as auditorias registradas para o pedido informado.
     * Esta operação somente consulta o banco e não acessa a AWS.
     */
    @GetMapping
    public ResponseEntity<List<AuditoriaRelatorioDTO>> listar(
            @PathVariable Long pedidoId
    ) {
        return ResponseEntity.ok(
                service.listarPorPedido(pedidoId)
        );
    }
}
