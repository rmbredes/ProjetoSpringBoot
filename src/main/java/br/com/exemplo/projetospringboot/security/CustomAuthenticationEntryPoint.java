package br.com.exemplo.projetospringboot.security;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;

import org.springframework.security.core.AuthenticationException;

import org.springframework.security.web.AuthenticationEntryPoint;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Produz uma resposta JSON padronizada quando uma requisição não está autenticada.
 */
@Component
public class CustomAuthenticationEntryPoint
        implements AuthenticationEntryPoint {

    /** Conversor responsável por gerar o corpo JSON da resposta. */
    private final ObjectMapper objectMapper;

    /**
     * @param objectMapper conversor JSON configurado pela aplicação
     */
    public CustomAuthenticationEntryPoint(
            ObjectMapper objectMapper
    ) {
        // Guarda o conversor que será usado quando a autenticação falhar.
        this.objectMapper = objectMapper;
    }

    /**
     * Inicia a resposta de erro para tokens ausentes, inválidos ou expirados.
     */
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {

        // Define o código HTTP apropriado para ausência de autenticação válida.
        response.setStatus(
                HttpServletResponse.SC_UNAUTHORIZED
        );

        // Informa que a resposta será representada em JSON.
        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        // Permite escrever corretamente os caracteres acentuados da mensagem.
        response.setCharacterEncoding(
                "UTF-8"
        );

        // Mantém a ordem dos campos na resposta enviada ao cliente.
        Map<String, Object> erro =
                new LinkedHashMap<>();

        // Registra quando a falha de autenticação ocorreu.
        erro.put(
                "dataHora",
                LocalDateTime.now()
        );

        // Inclui no corpo o mesmo código definido no cabeçalho HTTP.
        erro.put(
                "status",
                401
        );

        // Adiciona o nome técnico resumido do erro.
        erro.put(
                "erro",
                "Unauthorized"
        );

        // Explica ao cliente o motivo mais provável da rejeição.
        erro.put(
                "mensagem",
                "Autenticação necessária ou token inválido"
        );

        // Identifica o caminho que exigiu autenticação.
        erro.put(
                "path",
                request.getRequestURI()
        );

        // Converte o mapa em JSON e escreve o resultado na resposta HTTP.
        objectMapper.writeValue(
                response.getOutputStream(),
                erro
        );
    }
}
