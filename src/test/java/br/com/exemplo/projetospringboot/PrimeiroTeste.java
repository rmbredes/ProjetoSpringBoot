package br.com.exemplo.projetospringboot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Demonstra a estrutura mínima de um teste automatizado com JUnit.
 */
class PrimeiroTeste {

    /** Confirma que a soma de dois valores produz o resultado esperado. */
    @Test
    void deveSomarDoisNumeros() {

        // Organiza os dois valores usados no cenário.
        int numero1 = 10;
        int numero2 = 20;

        // Executa a operação que está sendo verificada.
        int resultado = numero1 + numero2;

        // Compara o resultado real com o valor esperado.
        assertEquals(
                30,
                resultado
        );
    }
}
