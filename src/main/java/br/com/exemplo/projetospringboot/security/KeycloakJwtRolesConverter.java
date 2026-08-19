package br.com.exemplo.projetospringboot.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Component
public class KeycloakJwtRolesConverter
        implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final String clientId;

    public KeycloakJwtRolesConverter(
            @Value("${app.security.keycloak.client-id}")
            String clientId
    ) {
        this.clientId = clientId;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {

        Map<String, Object> resourceAccess =
                jwt.getClaimAsMap("resource_access");

        if (resourceAccess == null) {
            return List.of();
        }

        Object acessoDoClient =
                resourceAccess.get(clientId);

        if (!(acessoDoClient instanceof Map<?, ?> clientClaims)) {
            return List.of();
        }

        Object rolesDoToken =
                clientClaims.get("roles");

        if (!(rolesDoToken instanceof Collection<?> roles)) {
            return List.of();
        }

        return roles
                .stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(String::toUpperCase)
                .map(role ->
                        (GrantedAuthority)
                                new SimpleGrantedAuthority(
                                        "ROLE_" + role
                                )
                )
                .toList();
    }
}