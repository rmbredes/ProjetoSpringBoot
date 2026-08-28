package br.com.exemplo.projetospringboot.entity;

import br.com.exemplo.projetospringboot.enums.StatusEventoOutbox;
import br.com.exemplo.projetospringboot.event.PedidoCriadoEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Testes unitários das regras internas da entidade EventoOutbox.
 */
class EventoOutboxTest {

    /**
     * Verifica se o evento passa para o status ERRO
     * quando atinge o limite de tentativas.
     */
    @Test
    void deveMarcarComoErroAoAtingirLimiteDeTentativas() {

        EventoOutbox eventoOutbox = new EventoOutbox(
                UUID.randomUUID(),
                PedidoCriadoEvent.class.getSimpleName(),
                "pedidos-criados",
                "40",
                "{}"
        );

        int limiteTentativas = 5;

        /*
         * Simula cinco falhas consecutivas de publicação.
         */
        for (int tentativa = 1;
             tentativa <= limiteTentativas;
             tentativa++) {

            eventoOutbox.registrarFalha(
                    "Kafka indisponível",
                    limiteTentativas
            );
        }

        /*
         * Depois da quinta falha, o evento não deve continuar
         * sendo selecionado automaticamente pelo job.
         */
        assertEquals(
                StatusEventoOutbox.ERRO,
                eventoOutbox.getStatus()
        );

        assertEquals(
                5,
                eventoOutbox.getQuantidadeTentativas()
        );

        assertNotNull(
                eventoOutbox.getUltimoErro()
        );

        assertEquals(
                "Kafka indisponível",
                eventoOutbox.getUltimoErro()
        );
    }
}