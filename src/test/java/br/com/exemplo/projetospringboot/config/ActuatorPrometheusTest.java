package br.com.exemplo.projetospringboot.config;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica a exposição e a segurança do endpoint utilizado
 * pelo Prometheus para coletar as métricas da aplicação.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ActuatorPrometheusTest {

    /*
     * Executa requisições HTTP simuladas utilizando
     * a configuração real de segurança da aplicação.
     */
    @Autowired
    private MockMvc mockMvc;

    @Test
    void devePermitirColetaDoPrometheusSemAutenticacao()
            throws Exception {

        /*
         * O servidor Prometheus precisa coletar as métricas
         * automaticamente, sem gerar e renovar JWT de usuário.
         */
        mockMvc.perform(
                        get("/actuator/prometheus")
                )
                .andExpect(
                        status().isOk()
                )

                /*
                 * Confirma que o endpoint público realmente exporta
                 * uma das métricas personalizadas da Outbox.
                 */
                .andExpect(
                        content().string(
                                containsString(
                                        "outbox_eventos_pendentes_eventos"
                                )
                        )
                );
    }

    @Test
    void deveRecusarMetricsParaUsuarioComum()
            throws Exception {

        /*
         * O endpoint metrics continua protegido porque fornece
         * informações detalhadas para diagnóstico manual.
         */
        mockMvc.perform(
                        get("/actuator/metrics")
                                .with(
                                        jwt()
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_USER"
                                                        )
                                                )
                                )
                )
                .andExpect(
                        status().isForbidden()
                );
    }

    @Test
    void devePermitirMetricsParaAdministrador()
            throws Exception {

        /*
         * Administradores continuam autorizados a utilizar
         * o endpoint JSON de diagnóstico do Actuator.
         */
        mockMvc.perform(
                        get("/actuator/metrics")
                                .with(
                                        jwt()
                                                .authorities(
                                                        new SimpleGrantedAuthority(
                                                                "ROLE_ADMIN"
                                                        )
                                                )
                                )
                )
                .andExpect(
                        status().isOk()
                );
    }
}
