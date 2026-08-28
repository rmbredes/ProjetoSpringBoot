package br.com.exemplo.projetospringboot.enums;

/**
 * Representa o estado de publicação de um evento
 * armazenado na tabela Outbox.
 */
public enum StatusEventoOutbox {

    /**
     * O evento foi salvo no banco, mas ainda não
     * foi confirmado pelo Kafka.
     */
    PENDENTE,

    /**
     * O Kafka confirmou a publicação do evento.
     */
    PUBLICADO,

    /**
     * O evento atingiu o limite de tentativas
     * e precisa de investigação.
     */
    ERRO
}