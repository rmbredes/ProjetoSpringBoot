/*
package br.com.exemplo.projetospringboot.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtRolesConverterTest {

    private final KeycloakJwtRolesConverter converter =
            new KeycloakJwtRolesConverter("projeto-springboot-api");

    @Test
    void deveConverterClientRolesParaAuthoritiesDoSpring() {
        Jwt jwt = jwtBuilder()
                .claim("resource_access", Map.of(
                        "projeto-springboot-api",
                        Map.of("roles", List.of("USER", "ADMIN"))
                ))
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void deveIgnorarRolesDeOutroClient() {
        Jwt jwt = jwtBuilder()
                .claim("resource_access", Map.of(
                        "outro-client",
                        Map.of("roles", List.of("ADMIN"))
                ))
                .build();

        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    void deveRetornarVazioQuandoResourceAccessNaoExistir() {
        Jwt jwt = jwtBuilder().build();

        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    void deveNormalizarRoleMinuscula() {
        Jwt jwt = jwtBuilder()
                .claim("resource_access", Map.of(
                        "projeto-springboot-api",
                        Map.of("roles", List.of("user"))
                ))
                .build();

        assertThat(converter.convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    private Jwt.Builder jwtBuilder() {
        Instant now = Instant.now();

        return Jwt.withTokenValue("token-teste")
                .header("alg", "RS256")
                .subject("usuario-teste")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300));
    }
}
*/


package br.com.exemplo.projetospringboot.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtRolesConverterTest {

    private final KeycloakJwtRolesConverter converter =
            new KeycloakJwtRolesConverter("projeto-springboot-api");

    @Test
    void deveConverterClientRolesParaAuthoritiesDoSpring() {
        Jwt jwt = jwtBuilder()
                .claim("resource_access", Map.of(
                        "projeto-springboot-api",
                        Map.of("roles", List.of("USER", "ADMIN"))
                ))
                .build();

        Collection<GrantedAuthority> authorities = converter.convert(jwt);

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void deveIgnorarRolesDeOutroClient() {
        Jwt jwt = jwtBuilder()
                .claim("resource_access", Map.of(
                        "outro-client",
                        Map.of("roles", List.of("ADMIN"))
                ))
                .build();

        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    void deveRetornarVazioQuandoResourceAccessNaoExistir() {
        Jwt jwt = jwtBuilder().build();

        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    void deveNormalizarRoleMinuscula() {
        Jwt jwt = jwtBuilder()
                .claim("resource_access", Map.of(
                        "projeto-springboot-api",
                        Map.of("roles", List.of("user"))
                ))
                .build();

        assertThat(converter.convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    private Jwt.Builder jwtBuilder() {
        Instant now = Instant.now();

        return Jwt.withTokenValue("token-teste")
                .header("alg", "RS256")
                .subject("usuario-teste")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300));
    }
}

