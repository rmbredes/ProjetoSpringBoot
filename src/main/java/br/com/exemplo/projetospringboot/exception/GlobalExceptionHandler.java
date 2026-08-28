package br.com.exemplo.projetospringboot.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /*
     * Logger utilizado para registrar detalhes técnicos dos erros.
     *
     * Esses detalhes aparecem no terminal da aplicação, mas não são
     * enviados ao cliente da API.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> tratarErroValidacao(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String mensagem = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        ErroResposta erro = new ErroResposta(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Erro de validação",
                mensagem,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(erro);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErroResposta> tratarNaoEncontrado(
            NoSuchElementException exception,
            HttpServletRequest request
    ) {

        ErroResposta erro = new ErroResposta(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                "Cliente não encontrado",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(erro);
    }

    /**
     * Trata qualquer exceção que não possua um tratamento mais específico.
     *
     * O erro técnico completo é registrado no terminal, enquanto
     * o cliente recebe uma mensagem genérica por segurança.
     *
     * @param exception exceção original ocorrida na aplicação
     * @param request requisição que estava sendo processada
     * @return resposta HTTP 500 sem detalhes internos sensíveis
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> tratarErroGeral(
            Exception exception,
            HttpServletRequest request
    ) {
        /*
         * Registra a causa completa no terminal da aplicação.
         */
        LOGGER.error(
                "Erro não tratado ao processar {}",
                request.getRequestURI(),
                exception
        );

        ErroResposta erro = new ErroResposta(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Erro interno da aplicação",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(erro);
    }

}
