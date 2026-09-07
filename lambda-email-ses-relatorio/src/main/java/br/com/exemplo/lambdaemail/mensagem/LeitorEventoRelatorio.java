package br.com.exemplo.lambdaemail.mensagem;

import br.com.exemplo.lambdaemail.dto.EventoRelatorioDisponivel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Converte a mensagem textual do SNS em um DTO Java.
 *
 * <p>Esta responsabilidade fica fora do handler para manter o fluxo principal
 * simples e para permitir testar a conversão do JSON separadamente.</p>
 */
public class LeitorEventoRelatorio {

    private final ObjectMapper objectMapper;

    /** Cria o leitor utilizado normalmente pela função Lambda. */
    public LeitorEventoRelatorio() {
        this(new ObjectMapper());
    }

    /**
     * Construtor útil para testes e futuras configurações do ObjectMapper.
     */
    LeitorEventoRelatorio(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * @param mensagemJson texto JSON recebido do SNS
     * @return evento convertido para um objeto Java
     */
    public EventoRelatorioDisponivel ler(String mensagemJson) {
        try {
            return objectMapper.readValue(
                    mensagemJson,
                    EventoRelatorioDisponivel.class
            );
        } catch (JsonProcessingException excecao) {
            /*
             * Lançar o erro é importante: a execução da Lambda falha e o SNS
             * pode tentar entregar novamente a mensagem conforme sua política.
             */
            throw new IllegalArgumentException(
                    "A mensagem recebida do SNS não contém um JSON válido",
                    excecao
            );
        }
    }
}
