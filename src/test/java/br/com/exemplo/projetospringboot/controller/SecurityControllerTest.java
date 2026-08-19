package br.com.exemplo.projetospringboot.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /*
     * Simula somente a validação criptográfica realizada com o Keycloak.
     * O filtro, o conversor de roles e as regras de acesso continuam reais.
     */
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void deveRetornar401QuandoNaoEnviarToken() throws Exception {
        mockMvc.perform(get("/clientes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.erro").value("Unauthorized"))
                .andExpect(jsonPath("$.mensagem")
                        .value("Autenticação necessária ou token inválido"));
    }

    @Test
    void deveRetornar401QuandoTokenForInvalido() throws Exception {
        when(jwtDecoder.decode("token-invalido"))
                .thenThrow(new BadJwtException("Token inválido"));

        mockMvc.perform(get("/clientes")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.erro").value("Unauthorized"));
    }

    @Test
    void devePermitirUserEmClientes() throws Exception {
        when(jwtDecoder.decode("token-user"))
                .thenReturn(jwtComClientRoles("token-user", "ricardo", "USER"));

        mockMvc.perform(get("/clientes")
                        .header("Authorization", "Bearer token-user"))
                .andExpect(status().isOk());
    }

    @Test
    void deveRetornar403QuandoUserAcessarAdmin() throws Exception {
        when(jwtDecoder.decode("token-user"))
                .thenReturn(jwtComClientRoles("token-user", "ricardo", "USER"));

        mockMvc.perform(get("/admin")
                        .header("Authorization", "Bearer token-user"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.erro").value("Forbidden"))
                .andExpect(jsonPath("$.mensagem")
                        .value("Você não possui permissão para acessar este recurso"));
    }

    @Test
    void devePermitirAdminEmAdmin() throws Exception {
        when(jwtDecoder.decode("token-admin"))
                .thenReturn(jwtComClientRoles(
                        "token-admin",
                        "admin-api",
                        "USER",
                        "ADMIN"
                ));

        mockMvc.perform(get("/admin")
                        .header("Authorization", "Bearer token-admin"))
                .andExpect(status().isOk())
                .andExpect(content().string("Acesso administrativo permitido"));
    }

    @Test
    void deveRetornar403QuandoTokenNaoPossuirRoleDoClient() throws Exception {
        when(jwtDecoder.decode("token-sem-role"))
                .thenReturn(jwtSemRoles("token-sem-role", "sem-role"));

        mockMvc.perform(get("/clientes")
                        .header("Authorization", "Bearer token-sem-role"))
                .andExpect(status().isForbidden());
    }

    private Jwt jwtComClientRoles(
            String token,
            String username,
            String... roles
    ) {
        return jwtBase(token, username)
                .claim("resource_access", Map.of(
                        "projeto-springboot-api",
                        Map.of("roles", List.of(roles))
                ))
                .build();
    }

    private Jwt jwtSemRoles(String token, String username) {
        return jwtBase(token, username).build();
    }

    private Jwt.Builder jwtBase(String token, String username) {
        Instant now = Instant.now();

        return Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .issuer("http://localhost:8081/realms/projeto-springboot")
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .claim("preferred_username", username);
    }
}
