package br.com.exemplo.projetospringboot.config;


import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Habilita o mecanismo de cache somente no perfil Docker,
 * no qual o Redis está disponível como infraestrutura externa.
 */
@Configuration
@EnableCaching
@Profile("docker")
public class CacheConfig {
}
