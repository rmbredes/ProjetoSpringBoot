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

/**
 * Gera tokens JWT assinados com a identidade e as permissões do usuário autenticado.
 */
@Service
public class JwtService {

    /** Codificador configurado com a chave usada para assinar o token. */
    private final JwtEncoder jwtEncoder;

    /** Tempo de validade do token, em segundos. */
    private final long expiration;

    /**
     * @param jwtEncoder codificador responsável pela assinatura do JWT
     * @param expiration validade configurada para o token, em segundos
     */
    public JwtService(
            JwtEncoder jwtEncoder,
            @Value("${jwt.expiration}") long expiration
    ) {
        // Guarda as configurações necessárias para gerar cada token.
        this.jwtEncoder = jwtEncoder;
        this.expiration = expiration;
    }

    /**
     * Gera um JWT para a autenticação validada pelo Spring Security.
     *
     * @param authentication autenticação já confirmada
     * @return token JWT assinado
     */
    public String gerarToken(Authentication authentication) {

        // Registra o instante inicial da validade do token.
        Instant agora = Instant.now();

        // Calcula a expiração usando o tempo definido na configuração.
        Instant expiracao = agora.plus(
                expiration,
                ChronoUnit.SECONDS
        );

        // Reúne as permissões do usuário em uma única claim textual.
        String roles = authentication
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        // Monta as informações que formarão o conteúdo do JWT.
        JwtClaimsSet claims = JwtClaimsSet
                .builder()
                .issuer("projeto-springboot")
                .issuedAt(agora)
                .expiresAt(expiracao)
                .subject(authentication.getName())
                .claim("roles", roles)
                .build();

        // Define o algoritmo usado para assinar o token.
        JwsHeader header = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        // Combina o cabeçalho de assinatura com as claims do usuário.
        JwtEncoderParameters parametros =
                JwtEncoderParameters.from(
                        header,
                        claims
                );

        // Assina, codifica e devolve apenas o valor textual do JWT.
        return jwtEncoder
                .encode(parametros)
                .getTokenValue();
    }

    /**
     * @return validade configurada para o token, em segundos
     */
    public long getExpiration() {
        return expiration;
    }

}
