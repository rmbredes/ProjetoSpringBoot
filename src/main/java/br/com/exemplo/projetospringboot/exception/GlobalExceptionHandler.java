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

/**
 * Converte exceções da aplicação em respostas HTTP padronizadas e seguras.
 */
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

    /**
     * Agrupa os erros de validação dos campos em uma resposta HTTP 400.
     *
     * @param ex exceção contendo os campos inválidos
     * @param request requisição que originou a validação
     * @return resposta padronizada com as mensagens de validação
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> tratarErroValidacao(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        // Extrai cada mensagem de campo e as une em um único texto.
        String mensagem = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        // Monta o corpo padronizado usando o caminho da requisição inválida.
        ErroResposta erro = new ErroResposta(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Erro de validação",
                mensagem,
                request.getRequestURI()
        );

        // Devolve o corpo acompanhado do status de requisição inválida.
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(erro);
    }

    /**
     * Trata buscas sem resultado como recurso não encontrado.
     *
     * @param exception exceção lançada pela ausência da entidade
     * @param request requisição que efetuou a busca
     * @return resposta HTTP 404 padronizada
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErroResposta> tratarNaoEncontrado(
            NoSuchElementException exception,
            HttpServletRequest request
    ) {

        // Constrói a descrição pública do recurso que não foi localizado.
        ErroResposta erro = new ErroResposta(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                "Cliente não encontrado",
                request.getRequestURI()
        );

        // Devolve o erro com o status HTTP correspondente.
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

        // Cria uma resposta genérica sem revelar detalhes internos da exceção.
        ErroResposta erro = new ErroResposta(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Erro interno da aplicação",
                request.getRequestURI()
        );

        // Envia ao cliente o status de erro interno e o corpo padronizado.
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(erro);
    }

}
