package br.com.exemplo.projetospringboot.config;

import br.com.exemplo.projetospringboot.security.CustomAccessDeniedHandler;
import br.com.exemplo.projetospringboot.security.CustomAuthenticationEntryPoint;
import br.com.exemplo.projetospringboot.security.CustomUserDetailsService;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.security.authentication.AuthenticationManager;
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

/*
 * Fornece matchers específicos para endpoints do Actuator.
 *
 * Essa abordagem acompanha a configuração do Actuator mesmo
 * se seu caminho padrão for alterado posteriormente.
 */
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;

/**
 * Reúne os componentes e as regras de autenticação e autorização da aplicação.
 */
@Configuration
public class SecurityConfig {

    /** Chave textual lida da configuração e usada na assinatura dos JWTs. */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /*
     * =========================================================
     * PASSWORD ENCODER
     * =========================================================
     */

    /** @return codificador BCrypt utilizado para proteger as senhas */
    @Bean
    public PasswordEncoder passwordEncoder() {

        // Cria o algoritmo de hash usado tanto no cadastro quanto no login.
        return new BCryptPasswordEncoder();
    }

    /*
     * =========================================================
     * AUTHENTICATION PROVIDER
     * =========================================================
     */

    /** Configura como usuário e senha serão consultados e comparados. */
    @Bean
    public AuthenticationProvider authenticationProvider(
            CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {

        // Cria o provedor que consulta usuários pelo serviço da aplicação.
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(
                        userDetailsService
                );

        // Ensina ao provedor como comparar a senha recebida com o hash salvo.
        provider.setPasswordEncoder(
                passwordEncoder
        );

        // Disponibiliza o provedor configurado ao Spring Security.
        return provider;
    }

    /*
     * =========================================================
     * AUTHENTICATION MANAGER
     * =========================================================
     */

    /** Obtém o gerenciador central que executa a autenticação. */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        // Reutiliza o gerenciador montado automaticamente pelo Spring.
        return configuration
                .getAuthenticationManager();
    }

    /*
     * =========================================================
     * JWT ENCODER
     * =========================================================
     */

    /** Cria o componente que assina os tokens JWT da aplicação. */
    @Bean
    public JwtEncoder jwtEncoder() {

        // Converte a chave textual em uma chave criptográfica HMAC-SHA256.
        SecretKey secretKey =
                new SecretKeySpec(
                        jwtSecret.getBytes(
                                StandardCharsets.UTF_8
                        ),
                        "HmacSHA256"
                );

        // Cria o codificador com uma fonte de chave imutável.
        return new NimbusJwtEncoder(
                new ImmutableSecret<>(secretKey)
        );
    }

    /*
     * =========================================================
     * JWT DECODER
     * =========================================================
     */

    /** Cria o componente que valida assinatura e conteúdo dos JWTs recebidos. */
    @Bean
    public JwtDecoder jwtDecoder() {

        // Reconstrói a mesma chave criptográfica usada durante a assinatura.
        SecretKey secretKey =
                new SecretKeySpec(
                        jwtSecret.getBytes(
                                StandardCharsets.UTF_8
                        ),
                        "HmacSHA256"
                );

        // Exige que os tokens tenham sido assinados com HMAC-SHA256.
        return NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(
                        MacAlgorithm.HS256
                )
                .build();
    }

    /*
     * =========================================================
     * CONVERSÃO DAS ROLES DO JWT
     * =========================================================
     */

    /** Converte a claim textual de roles em autoridades do Spring Security. */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {

        // Cria o conversor que será personalizado para nossa claim roles.
        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        // Define como extrair as permissões existentes dentro do JWT.
        converter.setJwtGrantedAuthoritiesConverter(
                jwt -> {

                    // Lê a claim criada pelo JwtService durante o login.
                    String roles =
                            jwt.getClaimAsString(
                                    "roles"
                            );

                    // Um token sem roles não recebe qualquer autoridade.
                    if (roles == null ||
                            roles.isBlank()) {

                        return List.of();
                    }

                    // Separa as roles e converte cada texto em uma autoridade.
                    return Arrays
                            .stream(
                                    roles.split(" ")
                            )
                            .map(role -> (GrantedAuthority) new SimpleGrantedAuthority(role))
                            .toList();
                }
        );

        // Disponibiliza o conversor personalizado à cadeia de segurança.
        return converter;
    }

    /*
     * =========================================================
     * SECURITY FILTER CHAIN
     * =========================================================
     */

    /** Configura os filtros e as regras de acesso aplicadas a cada requisição HTTP. */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationProvider authenticationProvider,
            JwtAuthenticationConverter jwtAuthenticationConverter,
            CustomAuthenticationEntryPoint authenticationEntryPoint,
            CustomAccessDeniedHandler accessDeniedHandler
    ) throws Exception {

        // Inicia a configuração encadeada da segurança HTTP.
        http

                // Desativa CSRF porque a API não mantém sessão no servidor.
                .csrf(
                        csrf ->
                                csrf.disable()
                )


                // Impede a criação de sessão e exige credenciais em cada requisição.
                .sessionManagement(
                        session ->
                                session.sessionCreationPolicy(
                                        SessionCreationPolicy.STATELESS
                                )
                )

                // Registra o provedor que autentica usuário e senha.
                .authenticationProvider(
                        authenticationProvider
                )

                // Padroniza as respostas para falhas de autenticação e autorização.
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

                // Define as permissões necessárias para cada grupo de endpoints.
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
                                /*
                                 * Restringe o endpoint de diagnóstico detalhado
                                 * de métricas aos administradores da aplicação.
                                 */
                                .requestMatchers(
                                        EndpointRequest.to(
                                                "metrics"
                                        )
                                )
                                .hasRole(
                                        "ADMIN"
                                )

                                /*
                                 * Permite que o servidor Prometheus realize coletas automáticas
                                 * sem depender de um JWT de usuário, que possui expiração.
                                 *
                                 * Em produção, esse endpoint deverá ficar disponível somente
                                 * dentro da rede utilizada pela infraestrutura de monitoramento.
                                 */
                                .requestMatchers(
                                        EndpointRequest.to(
                                                "prometheus"
                                        )
                                )
                                .permitAll()
                                /*
                                 * Permite que ferramentas de infraestrutura consultem
                                 * o health check sem precisar obter e renovar um JWT.
                                 *
                                 * A exposição de componentes e detalhes continua controlada
                                 * por show-components e show-details no application.yaml.
                                 */
                                .requestMatchers(
                                        EndpointRequest.to("health")
                                )
                                .permitAll()

                                .anyRequest()
                                .authenticated()
                )

                // Habilita o processamento de JWT no cabeçalho Bearer.
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

        // Constrói e devolve a cadeia de filtros configurada.
        return http.build();
    }
}
