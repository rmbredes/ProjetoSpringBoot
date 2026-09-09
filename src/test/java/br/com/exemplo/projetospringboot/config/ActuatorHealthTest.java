package br.com.exemplo.projetospringboot.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica o contrato de segurança e visibilidade
 * definido para os endpoints do Actuator.
 *
 * O teste utiliza a aplicação completa para incluir
 * tanto o Actuator quanto a SecurityFilterChain real.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ActuatorHealthTest {

    /*
     * Simula requisições HTTP sem precisar iniciar
     * um servidor em uma porta real.
     */
    @Autowired
    private MockMvc mockMvc;

    @Test
    void devePermitirHealthSemTokenMasOcultarComponentes()
            throws Exception {

        /*
         * Realiza uma requisição anônima para confirmar que
         * ferramentas de infraestrutura não precisam de JWT.
         */
        mockMvc.perform(
                        get("/actuator/health")
                )

                /*
                 * Como os indicadores usados no teste estão saudáveis,
                 * o endpoint deve responder HTTP 200.
                 */
                .andExpect(
                        status().isOk()
                )

                /*
                 * O estado geral pode ser consultado publicamente.
                 */
                .andExpect(
                        jsonPath("$.status")
                                .value("UP")
                )

                /*
                 * Informações internas não devem ser exibidas
                 * para uma requisição anônima.
                 */
                .andExpect(
                        jsonPath("$.components")
                                .doesNotExist()
                );
    }

    @Test
    void deveExibirComponentesParaUsuarioAutenticado()
            throws Exception {

        /*
         * O jwt() cria uma autenticação simulada.
         *
         * Não precisamos gerar nem assinar um token real neste teste,
         * pois o objetivo é validar autorização e resposta do endpoint.
         */
        mockMvc.perform(
                        get("/actuator/health")

                                .with(
                                        jwt()
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_USER"
                                                        )
                                                )
                                )
                )

                /*
                 * Um usuário autenticado pode consultar o health.
                 */
                .andExpect(
                        status().isOk()
                )

                /*
                 * O componente do banco deve estar disponível
                 * porque o teste utiliza um PostgreSQL temporário
                 * iniciado automaticamente pelo Testcontainers.
                 */
                .andExpect(
                        jsonPath("$.components.db.status")
                                .value("UP")
                )

                /*
                 * Confirma que os detalhes técnicos ficaram
                 * visíveis para o usuário autenticado.
                 */
                .andExpect(
                        jsonPath("$.components.db.details.database")
                                .value("PostgreSQL")
                );
    }

    @Test
    void deveProtegerPaginaDeDescobertaSemToken()
            throws Exception {

        /*
         * A liberação foi feita especificamente para health.
         * A página geral do Actuator continua protegida.
         */
        mockMvc.perform(
                        get("/actuator")
                )

                /*
                 * Sem JWT, nossa SecurityFilterChain deve
                 * impedir o acesso à página de descoberta.
                 */
                .andExpect(
                        status().isUnauthorized()
                )

                /*
                 * Confirma que a resposta continua seguindo
                 * o padrão personalizado da nossa API.
                 */
                .andExpect(
                        jsonPath("$.status")
                                .value(401)
                );
    }
}