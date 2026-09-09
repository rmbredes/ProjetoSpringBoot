package br.com.exemplo.projetospringboot.entity;

/**
 * Situação resumida do pagamento dentro do monólito.
 *
 * Os dados completos continuam pertencendo ao pagamento-service.
 */
public enum StatusPagamentoPedido {

    /** Pedido criado, mas pagamento ainda não concluído. */
    AGUARDANDO,

    /** Pagamento confirmado pelo pagamento-service. */
    APROVADO,

    /** Pagamento recusado pelo pagamento-service. */
    RECUSADO
}