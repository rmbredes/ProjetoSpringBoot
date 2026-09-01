package br.com.exemplo.projetospringboot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifica se o contexto completo da aplicação pode ser inicializado.
 */
@SpringBootTest
class ProjetoSpringBootApplicationTests {

    /**
     * O teste é aprovado quando o Spring cria todos os beans sem lançar exceção.
     */
    @Test
    void contextLoads() {
        // Não há execução adicional: a própria inicialização do contexto é a verificação.
    }

}
