package br.com.exemplo.projetospringboot.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;

    private final long expiration;

    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${jwt.expiration}") long expiration
    ) {
        this.jwtEncoder = jwtEncoder;
        this.expiration = expiration;
    }

    public String gerarToken(Authentication authentication) {

        Instant agora = Instant.now();

        Instant expiracao = agora.plus(
                expiration,
                ChronoUnit.SECONDS
        );

        String roles = authentication
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet
                .builder()
                .issuer("projeto-springboot")
                .issuedAt(agora)
                .expiresAt(expiracao)
                .subject(authentication.getName())
                .claim("roles", roles)
                .build();

        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        JwtEncoderParameters parametros =
                JwtEncoderParameters.from(
                        header,
                        claims
                );

        return jwtEncoder
                .encode(parametros)
                .getTokenValue();
    }

    public long getExpiration() {
        return expiration;
    }

}
