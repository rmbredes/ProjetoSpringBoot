package br.com.exemplo.projetospringboot.config;

import br.com.exemplo.projetospringboot.security.CustomAccessDeniedHandler;
import br.com.exemplo.projetospringboot.security.CustomAuthenticationEntryPoint;
import br.com.exemplo.projetospringboot.security.KeycloakJwtRolesConverter;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    /*
     * Conecta nosso conversor de roles ao mecanismo
     * de autenticação JWT do Spring Security.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter(
            KeycloakJwtRolesConverter rolesConverter
    ) {

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        /*
         * Por padrão, o Spring usa o claim "sub"
         * como nome do usuário.
         *
         * O Keycloak também fornece
         * "preferred_username", que é mais legível.
         */
        converter.setPrincipalClaimName(
                "preferred_username"
        );

        converter.setJwtGrantedAuthoritiesConverter(
                rolesConverter
        );

        return converter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            CustomAuthenticationEntryPoint authenticationEntryPoint,
            CustomAccessDeniedHandler accessDeniedHandler
    ) throws Exception {

        http

                /*
                 * A API não utiliza formulário ou sessão do navegador.
                 * As requisições serão autenticadas pelo bearer token.
                 */
                .csrf(
                        csrf -> csrf.disable()
                )

                /*
                 * Cada requisição deve trazer seu próprio token.
                 * O servidor não guarda sessão autenticada.
                 */
                .sessionManagement(
                        session ->
                                session.sessionCreationPolicy(
                                        SessionCreationPolicy.STATELESS
                                )
                )

                /*
                 * Mantemos as respostas JSON personalizadas
                 * que o projeto já possuía para 401 e 403.
                 */
                .exceptionHandling(
                        exception -> exception
                                .authenticationEntryPoint(
                                        authenticationEntryPoint
                                )
                                .accessDeniedHandler(
                                        accessDeniedHandler
                                )
                )

                /*
                 * Regras de autorização.
                 */
                .authorizeHttpRequests(
                        authorize -> authorize

                                .requestMatchers(
                                        "/clientes/**",
                                        "/pedidos/**"
                                )
                                .hasAnyRole(
                                        "USER",
                                        "ADMIN"
                                )

                                .requestMatchers(
                                        "/admin/**"
                                )
                                .hasRole(
                                        "ADMIN"
                                )

                                .anyRequest()
                                .authenticated()
                )

                /*
                 * Transforma a aplicação em um
                 * OAuth 2.0 Resource Server.
                 */
                .oauth2ResourceServer(
                        oauth2 -> oauth2

                                .authenticationEntryPoint(
                                        authenticationEntryPoint
                                )

                                .accessDeniedHandler(
                                        accessDeniedHandler
                                )

                                .jwt(
                                        jwt ->
                                                jwt.jwtAuthenticationConverter(
                                                        jwtAuthenticationConverter
                                                )
                                )
                );

        return http.build();
    }
}


//IMPLEMETAÇÃO JWT LOCAL
/*
import org.springframework.security.authentication.AuthenticationProvider;

import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import org.springframework.security.config.Customizer;

import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

     * =========================================================
     * PASSWORD ENCODER
     * =========================================================



    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

     * =========================================================
     * AUTHENTICATION PROVIDER
     * =========================================================



    @Bean
    public AuthenticationProvider authenticationProvider(
            CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(
                        userDetailsService
                );

        provider.setPasswordEncoder(
                passwordEncoder
        );

        return provider;
    }

     * =========================================================
     * AUTHENTICATION MANAGER
     * =========================================================



    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration
                .getAuthenticationManager();
    }

     * =========================================================
     * JWT ENCODER
     * =========================================================



    @Bean
    public JwtEncoder jwtEncoder() {

        SecretKey secretKey =
                new SecretKeySpec(
                        jwtSecret.getBytes(
                                StandardCharsets.UTF_8
                        ),
                        "HmacSHA256"
                );

        return new NimbusJwtEncoder(
                new ImmutableSecret<>(secretKey)
        );
    }

     * =========================================================
     * JWT DECODER
     * =========================================================



    @Bean
    public JwtDecoder jwtDecoder() {

        SecretKey secretKey =
                new SecretKeySpec(
                        jwtSecret.getBytes(
                                StandardCharsets.UTF_8
                        ),
                        "HmacSHA256"
                );

        return NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(
                        MacAlgorithm.HS256
                )
                .build();
    }

     * =========================================================
     * CONVERSÃO DAS ROLES DO JWT
     * =========================================================



    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                jwt -> {

                    String roles =
                            jwt.getClaimAsString(
                                    "roles"
                            );

                    if (roles == null ||
                            roles.isBlank()) {

                        return List.of();
                    }

                    return Arrays
                            .stream(
                                    roles.split(" ")
                            )
                            .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role))
                            .toList();
                }
        );

        return converter;
    }

     * =========================================================
     * SECURITY FILTER CHAIN
     * =========================================================



    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationProvider authenticationProvider,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            CustomAuthenticationEntryPoint authenticationEntryPoint,
            CustomAccessDeniedHandler accessDeniedHandler
    ) throws Exception {

        http

                .csrf(
                        csrf ->
                                csrf.disable()
                )

                .sessionManagement(
                        session ->
                                session.sessionCreationPolicy(
                                        SessionCreationPolicy.STATELESS
                                )
                )

                .authenticationProvider(
                        authenticationProvider
                )

                .exceptionHandling(
                        exception ->
                                exception

                                        .authenticationEntryPoint(
                                                authenticationEntryPoint
                                        )

                                        .accessDeniedHandler(
                                                accessDeniedHandler
                                        )
                )

                .authorizeHttpRequests(
                        authorize -> authorize

                                .requestMatchers(
                                        "/auth/login",
                                        "/auth/cadastrar"
                                )
                                .permitAll()

                                .requestMatchers(
                                        "/clientes/**"
                                )
                                .hasAnyRole(
                                        "USER",
                                        "ADMIN"
                                )

                                .requestMatchers(
                                        "/pedidos/**"
                                )
                                .hasAnyRole(
                                        "USER",
                                        "ADMIN"
                                )

                                .requestMatchers(
                                        "/admin/**"
                                )
                                .hasRole(
                                        "ADMIN"
                                )

                                .anyRequest()
                                .authenticated()
                )

                .oauth2ResourceServer(
                        oauth2 ->
                                oauth2

                                        .authenticationEntryPoint(
                                                authenticationEntryPoint
                                        )

                                        .jwt(
                                                jwt ->
                                                        jwt.jwtAuthenticationConverter(
                                                                jwtAuthenticationConverter
                                                        )
                                        )
                );

        return http.build();
    }
}
*/
