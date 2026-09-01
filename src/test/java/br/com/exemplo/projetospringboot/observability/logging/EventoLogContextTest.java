package br.com.exemplo.projetospringboot.observability.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifica a inclusão e a remoção automática
 * do eventoId no contexto dos logs.
 */
class EventoLogContextTest {

    /**
     * Impede que um teste deixe dados no MDC
     * para outro teste executado na mesma thread.
     */
    @AfterEach
    void limparContexto() {
        MDC.clear();
    }

    @Test
    void deveAdicionarERemoverEventoIdDoMdc() {

        UUID eventoId = UUID.randomUUID();

        /*
         * Antes da abertura, a thread não deve
         * possuir um eventoId associado.
         */
        assertNull(
                MDC.get(
                        EventoLogContext.CHAVE_EVENTO_ID
                )
        );

        /*
         * O try-with-resources fechará o contexto
         * automaticamente ao sair deste bloco.
         */
        try (
                MDC.MDCCloseable contexto =
                        EventoLogContext.abrir(
                                eventoId
                        )
        ) {
            /*
             * Dentro do bloco, o identificador deve
             * estar disponível no MDC como texto.
             */
            assertEquals(
                    eventoId.toString(),
                    MDC.get(
                            EventoLogContext.CHAVE_EVENTO_ID
                    )
            );
        }

        /*
         * Depois do fechamento, o identificador
         * precisa ter sido removido da thread.
         */
        assertNull(
                MDC.get(
                        EventoLogContext.CHAVE_EVENTO_ID
                )
        );
    }
}