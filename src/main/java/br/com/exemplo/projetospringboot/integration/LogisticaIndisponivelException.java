package br.com.exemplo.projetospringboot.integration;

/**
 * Representa uma falha temporária na comunicação
 * entre o monólito e o logistica-service.
 *
 * Essa exceção pode ocorrer quando:
 *
 * - o logistica-service está desligado;
 * - a conexão HTTP falha;
 * - o serviço devolve um erro;
 * - o Circuit Breaker está aberto e bloqueia a chamada.
 *
 * O pedido não será perdido. Ele continuará com a situação
 * logística PENDENTE e poderá ser processado novamente pelo job.
 */
public class LogisticaIndisponivelException
        extends RuntimeException {

    public LogisticaIndisponivelException(
            String mensagem,
            Throwable causa
    ) {
        super(mensagem, causa);
    }
}