package br.com.exemplo.projetospringboot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class ProjetoSpringBootApplicationTests {

    /* Impede que o teste dependa de um Keycloak em execução. */
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void deveCarregarContextoDaAplicacao() {
    }
}
