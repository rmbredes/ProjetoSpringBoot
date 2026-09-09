package br.com.exemplo.projetospringboot.service;

import br.com.exemplo.projetospringboot.entity.Pedido;
import br.com.exemplo.projetospringboot.entity.StatusPagamentoPedido;
import br.com.exemplo.projetospringboot.event.PagamentoProcessadoEvent;
import br.com.exemplo.projetospringboot.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Atualiza o resumo do pedido a partir dos eventos
 * publicados pelo pagamento-service.
 */
@Service
public class ProcessamentoPagamentoService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    ProcessamentoPagamentoService.class
            );

    private final PedidoRepository pedidoRepository;

    public ProcessamentoPagamentoService(
            PedidoRepository pedidoRepository
    ) {
        this.pedidoRepository = pedidoRepository;
    }

    /**
     * Processa de forma idempotente o resultado do pagamento.
     *
     * @return true quando o pedido foi alterado agora;
     *         false quando o mesmo resultado já havia sido recebido
     */
    @Transactional
    public boolean processar(
            PagamentoProcessadoEvent evento
    ) {
        Pedido pedido = pedidoRepository
                .findById(evento.pedidoId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Pedido não encontrado: "
                                        + evento.pedidoId()
                        )
                );

        StatusPagamentoPedido novoStatus =
                StatusPagamentoPedido.valueOf(
                        evento.status()
                );

        /*
         * O Kafka pode entregar a mesma mensagem novamente.
         * Se o resultado já estiver registrado, não repetimos o efeito.
         */
        if (Objects.equals(
                pedido.getPagamentoId(),
                evento.pagamentoId()
        ) && pedido.getStatusPagamento() == novoStatus) {

            LOGGER.info(
                    "Resultado de pagamento já processado: " +
                            "eventoId={}, pagamentoId={}, pedidoId={}",
                    evento.eventoId(),
                    evento.pagamentoId(),
                    evento.pedidoId()
            );

            return false;
        }

        pedido.registrarResultadoPagamento(
                evento.eventoId(),
                evento.pagamentoId(),
                novoStatus
        );

        /*
         * Não precisamos chamar save().
         *
         * A entidade foi carregada dentro da transação e o JPA
         * executará o UPDATE pelo mecanismo de dirty checking.
         */
        LOGGER.info(
                "Resultado de pagamento registrado: " +
                        "eventoId={}, pagamentoId={}, pedidoId={}, " +
                        "statusPagamento={}, statusLogistica={}",
                evento.eventoId(),
                evento.pagamentoId(),
                evento.pedidoId(),
                pedido.getStatusPagamento(),
                pedido.getStatusLogistica()
        );

        return true;
    }
}