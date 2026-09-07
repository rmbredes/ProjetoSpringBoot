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

import br.com.exemplo.projetospringboot.exception.ArmazenamentoAnexoException;
import br.com.exemplo.projetospringboot.exception.ArquivoAnexoInvalidoException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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
                /*
                 * Cada serviço informa qual recurso não foi encontrado.
                 *
                 * Caso uma exceção antiga não possua mensagem, usamos
                 * uma descrição genérica.
                 */
                exception.getMessage() == null
                        || exception.getMessage().isBlank()
                        ? "Recurso não encontrado"
                        : exception.getMessage(),
                request.getRequestURI()
        );

        // Devolve o erro com o status HTTP correspondente.
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(erro);
    }

    /**
     * Converte violações das regras de arquivo em HTTP 400.
     */
    @ExceptionHandler(ArquivoAnexoInvalidoException.class)
    public ResponseEntity<ErroResposta> tratarArquivoInvalido(
            ArquivoAnexoInvalidoException exception,
            HttpServletRequest request
    ) {
        ErroResposta erro = new ErroResposta(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Arquivo inválido",
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(erro);
    }

    /**
     * Trata arquivos rejeitados pelo próprio limite multipart do Spring.
     *
     * <p>HTTP 413 significa Payload Too Large.</p>
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErroResposta> tratarArquivoMuitoGrande(
            MaxUploadSizeExceededException exception,
            HttpServletRequest request
    ) {
        ErroResposta erro = new ErroResposta(
                LocalDateTime.now(),
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "Arquivo muito grande",
                "O arquivo deve possuir no máximo 10 MB",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(erro);
    }

    /**
     * Converte falhas de comunicação com o armazenamento em HTTP 502.
     *
     * <p>O cliente recebe uma mensagem segura. A causa técnica
     * completa permanece disponível nos logs.</p>
     */
    @ExceptionHandler(ArmazenamentoAnexoException.class)
    public ResponseEntity<ErroResposta> tratarFalhaArmazenamento(
            ArmazenamentoAnexoException exception,
            HttpServletRequest request
    ) {
        LOGGER.error(
                "Falha no armazenamento do anexo: caminho={}",
                request.getRequestURI(),
                exception
        );

        ErroResposta erro = new ErroResposta(
                LocalDateTime.now(),
                HttpStatus.BAD_GATEWAY.value(),
                "Falha no armazenamento",
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(erro);
    }

    /**
     * Converte uma falha de publicação do relatório em HTTP 502.
     *
     * <p>O código 502 informa que nossa API recebeu a requisição,
     * mas não conseguiu concluir a comunicação com o serviço externo.</p>
     */
    @ExceptionHandler(PublicacaoRelatorioException.class)
    public ResponseEntity<ErroResposta> tratarFalhaPublicacaoRelatorio(
            PublicacaoRelatorioException exception,
            HttpServletRequest request
    ) {
        LOGGER.error(
                "Falha ao publicar relatório no SQS: caminho={}",
                request.getRequestURI(),
                exception
        );

        ErroResposta erro = new ErroResposta(
                LocalDateTime.now(),
                HttpStatus.BAD_GATEWAY.value(),
                "Falha na mensageria",
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(erro);
    }

    /**
     * Responde HTTP 409 quando o recurso existe, mas seu processamento
     * assíncrono ainda não chegou ao estado necessário para download.
     */
    @ExceptionHandler(RelatorioIndisponivelException.class)
    public ResponseEntity<ErroResposta> tratarRelatorioIndisponivel(
            RelatorioIndisponivelException exception,
            HttpServletRequest request
    ) {
        ErroResposta erro = new ErroResposta(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Relatório indisponível",
                exception.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
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
