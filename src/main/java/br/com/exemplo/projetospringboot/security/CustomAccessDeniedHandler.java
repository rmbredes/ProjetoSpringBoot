package br.com.exemplo.projetospringboot.security;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;

import org.springframework.security.access.AccessDeniedException;

import org.springframework.security.web.access.AccessDeniedHandler;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Produz uma resposta JSON padronizada quando o usuário autenticado não possui permissão.
 */
@Component
public class CustomAccessDeniedHandler
        implements AccessDeniedHandler {

    /** Conversor utilizado para escrever o corpo JSON diretamente na resposta HTTP. */
    private final ObjectMapper objectMapper;

    /**
     * @param objectMapper conversor JSON configurado pela aplicação
     */
    public CustomAccessDeniedHandler(
            ObjectMapper objectMapper
    ) {
        // Mantém o conversor disponível para o tratamento das respostas negadas.
        this.objectMapper = objectMapper;
    }

    /**
     * Trata acessos autenticados que não possuem a autorização exigida pelo recurso.
     */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {

        // Define o código HTTP que representa falta de permissão.
        response.setStatus(
                HttpServletResponse.SC_FORBIDDEN
        );

        // Informa ao cliente que o corpo será devolvido em JSON.
        response.setContentType(
                MediaType.APPLICATION_JSON_VALUE
        );

        // Garante a codificação correta dos caracteres da mensagem.
        response.setCharacterEncoding(
                "UTF-8"
        );

        // Preserva a ordem dos campos para deixar a resposta previsível.
        Map<String, Object> erro =
                new LinkedHashMap<>();

        // Registra o instante em que a recusa ocorreu.
        erro.put(
                "dataHora",
                LocalDateTime.now()
        );

        // Repete no corpo o status HTTP da resposta.
        erro.put(
                "status",
                403
        );

        // Informa a descrição técnica resumida do erro.
        erro.put(
                "erro",
                "Forbidden"
        );

        // Oferece ao cliente uma mensagem compreensível sobre a recusa.
        erro.put(
                "mensagem",
                "Você não possui permissão para acessar este recurso"
        );

        // Identifica o recurso que o usuário tentou acessar.
        erro.put(
                "path",
                request.getRequestURI()
        );

        // Serializa o mapa e o escreve no corpo da resposta HTTP.
        objectMapper.writeValue(
                response.getOutputStream(),
                erro
        );
    }
}
