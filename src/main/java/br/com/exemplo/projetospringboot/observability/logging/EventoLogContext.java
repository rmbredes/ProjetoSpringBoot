package br.com.exemplo.projetospringboot.observability.logging;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Centraliza a inclusão do eventoId no contexto dos logs.
 *
 * O MDC mantém informações associadas à thread atual.
 * Enquanto o eventoId estiver presente, o padrão de logging
 * poderá incluí-lo automaticamente nas mensagens.
 */
public final class EventoLogContext {

    /**
     * Nome único utilizado para armazenar o identificador no MDC.
     *
     * A constante evita que partes diferentes do projeto
     * utilizem nomes como eventoId, eventId ou evento_id.
     */
    public static final String CHAVE_EVENTO_ID =
            "eventoId";

    /**
     * Impede a criação de objetos desta classe.
     *
     * Seus métodos são estáticos e não dependem do Spring.
     */
    private EventoLogContext() {
    }

    /**
     * Abre um contexto de log para determinado evento.
     *
     * O retorno deve ser utilizado em um try-with-resources.
     * Ao fechar o contexto, o eventoId será removido da thread.
     *
     * @param eventoId identificador que correlacionará os logs
     * @return contexto responsável pela limpeza automática
     */
    public static MDC.MDCCloseable abrir(
            UUID eventoId
    ) {
        /*
         * O MDC armazena textos, portanto o UUID
         * precisa ser convertido para String.
         */
        return MDC.putCloseable(
                CHAVE_EVENTO_ID,
                eventoId.toString()
        );
    }
}