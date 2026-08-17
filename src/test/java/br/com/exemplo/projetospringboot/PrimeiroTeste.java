package br.com.exemplo.projetospringboot;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrimeiroTeste {

    @Test
    void deveSomarDoisNumeros() {

        int numero1 = 10;
        int numero2 = 20;

        int resultado = numero1 + numero2;

        assertEquals(
                30,
                resultado
        );
    }
}