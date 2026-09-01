package br.com.exemplo.projetospringboot.controller;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testa a autenticação JWT e as regras de autorização dos endpoints protegidos.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityControllerTest {

    /** Cliente HTTP de teste configurado com toda a cadeia de segurança. */
    @Autowired
    private MockMvc mockMvc;

    /** Codificador real usado para produzir tokens válidos nos cenários. */
    @Autowired
    private JwtEncoder jwtEncoder;

    /** Confirma que as dependências essenciais do teste foram inicializadas. */
    @Test
    void testeBasicoMockMvc() {

        System.out.println(
                ">>> SECURITY CONTROLLER TEST ENTROU AQUI <<<"
        );

        System.out.println(
                "MockMvc = " + mockMvc
        );

        System.out.println(
                "JwtEncoder = " + jwtEncoder
        );
    }


    // =====================================================
    // 401 - SEM TOKEN
    // =====================================================



    /** Confirma que um recurso protegido rejeita uma requisição sem token. */
    @Test
    void deveRetornar401QuandoNaoEnviarToken()
            throws Exception {

        mockMvc.perform(
                        get("/clientes")
                )

                .andExpect(
                        status().isUnauthorized()
                )

                .andExpect(
                        jsonPath("$.status")
                                .value(401)
                )

                .andExpect(
                        jsonPath("$.erro")
                                .value("Unauthorized")
                )

                .andExpect(
                        jsonPath("$.mensagem")
                                .value(
                                        "Autenticação necessária ou token inválido"
                                )
                );
        System.out.println(
                ">>> EXECUTOU <<<"
        );
    }

    // =====================================================
    // 401 - JWT INVÁLIDO
    // =====================================================

    /** Confirma que um texto qualquer não é aceito como JWT. */
    @Test
    void deveRetornar401QuandoJwtForInvalido()
            throws Exception {

        mockMvc.perform(
                        get("/clientes")

                                .header(
                                        "Authorization",
                                        "Bearer token-invalido"
                                )
                )

                .andExpect(
                        status().isUnauthorized()
                )

                .andExpect(
                        jsonPath("$.status")
                                .value(401)
                )

                .andExpect(
                        jsonPath("$.erro")
                                .value("Unauthorized")
                );
    }

    // =====================================================
    // JWT VÁLIDO + ROLE_USER
    // =====================================================

    /** Confirma que um usuário comum acessa um endpoint permitido à sua role. */
    @Test
    void devePermitirAcessoAClientesComJwtValido()
            throws Exception {

        String token =
                gerarToken(
                        "ricardo",
                        "ROLE_USER"
                );

        mockMvc.perform(
                        get("/clientes")

                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )

                .andExpect(
                        status().isOk()
                );
    }

    // =====================================================
    // 403 - JWT VÁLIDO, MAS SEM ROLE_ADMIN
    // =====================================================

    /** Confirma que autenticação válida não substitui a autorização exigida. */
    @Test
    void deveRetornar403QuandoUsuarioNaoForAdmin()
            throws Exception {

        String token =
                gerarToken(
                        "ricardo",
                        "ROLE_USER"
                );

        mockMvc.perform(
                        get("/admin")

                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )

                .andExpect(
                        status().isForbidden()
                )

                .andExpect(
                        jsonPath("$.status")
                                .value(403)
                )

                .andExpect(
                        jsonPath("$.erro")
                                .value("Forbidden")
                )

                .andExpect(
                        jsonPath("$.mensagem")
                                .value(
                                        "Você não possui permissão para acessar este recurso"
                                )
                );
    }

    // =====================================================
    // ROLE_ADMIN - ACESSO PERMITIDO
    // =====================================================

    /** Confirma que a role administrativa libera o endpoint de administração. */
    @Test
    void devePermitirAcessoQuandoUsuarioForAdmin()
            throws Exception {

        String token =
                gerarToken(
                        "admin",
                        "ROLE_ADMIN"
                );

        mockMvc.perform(
                        get("/admin")

                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )

                .andExpect(
                        status().isOk()
                )

                .andExpect(
                        content()
                                .string(
                                        "Acesso administrativo permitido"
                                )
                );
    }

    // =====================================================
    // JWT EXPIRADO
    // =====================================================

    /** Confirma que um JWT corretamente assinado deixa de valer após a expiração. */
    @Test
    void deveRetornar401QuandoJwtEstiverExpirado()
            throws Exception {

        String token =
                gerarTokenExpirado(
                        "ricardo",
                        "ROLE_USER"
                );

        mockMvc.perform(
                        get("/clientes")

                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                )

                .andExpect(
                        status().isUnauthorized()
                );
    }

    // =====================================================
    // MÉTODO AUXILIAR - TOKEN VÁLIDO
    // =====================================================

    /** Gera um token assinado e válido por uma hora para os testes. */
    private String gerarToken(
            String usuario,
            String roles
    ) {

        Instant agora =
                Instant.now();

        JwtClaimsSet claims =
                JwtClaimsSet
                        .builder()

                        .issuer(
                                "projeto-springboot"
                        )

                        .subject(
                                usuario
                        )

                        .issuedAt(
                                agora
                        )

                        .expiresAt(
                                agora.plus(
                                        1,
                                        ChronoUnit.HOURS
                                )
                        )

                        .claim(
                                "roles",
                                roles
                        )

                        .build();

        JwsHeader header =
                JwsHeader
                        .with(
                                MacAlgorithm.HS256
                        )
                        .build();

        return jwtEncoder
                .encode(
                        JwtEncoderParameters.from(
                                header,
                                claims
                        )
                )
                .getTokenValue();
    }

    // =====================================================
    // MÉTODO AUXILIAR - TOKEN EXPIRADO
    // =====================================================

    /** Gera um token assinado cuja validade terminou antes da requisição. */
    private String gerarTokenExpirado(
            String usuario,
            String roles
    ) {

        Instant agora =
                Instant.now();

        JwtClaimsSet claims =
                JwtClaimsSet
                        .builder()

                        .issuer(
                                "projeto-springboot"
                        )

                        .subject(
                                usuario
                        )

                        .issuedAt(
                                agora.minus(
                                        2,
                                        ChronoUnit.HOURS
                                )
                        )

                        .expiresAt(
                                agora.minus(
                                        1,
                                        ChronoUnit.HOURS
                                )
                        )

                        .claim(
                                "roles",
                                roles
                        )

                        .build();

        JwsHeader header =
                JwsHeader
                        .with(
                                MacAlgorithm.HS256
                        )
                        .build();

        return jwtEncoder
                .encode(
                        JwtEncoderParameters.from(
                                header,
                                claims
                        )
                )
                .getTokenValue();
    }
}
