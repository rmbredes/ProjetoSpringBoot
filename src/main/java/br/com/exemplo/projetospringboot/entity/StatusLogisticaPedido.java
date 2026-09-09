package br.com.exemplo.projetospringboot.entity;

/**
 * Situação resumida da integração do pedido com a logística.
 */
public enum StatusLogisticaPedido {

    /** A logística ainda não deve ser acionada. */
    NAO_SOLICITADA,

    /** Pagamento aprovado e chamada à logística pendente. */
    PENDENTE,

    /** Logística confirmou a criação da entrega. */
    AUTORIZADA
}