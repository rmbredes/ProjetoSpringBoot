package br.com.exemplo.projetospringboot.messaging.recovery;

import br.com.exemplo.projetospringboot.event.PedidoCriadoEvent;
import br.com.exemplo.projetospringboot.observability.logging.EventoLogContext;
import br.com.exemplo.projetospringboot.observability.metrics.DltMetrics;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.listener.ConsumerAwareRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;

/**
 * Acrescenta observabilidade ao processo de recuperação
 * das mensagens encaminhadas para a DLT.
 *
 * O envio real continua sendo responsabilidade do
 * DeadLetterPublishingRecoverer fornecido pelo Spring Kafka.
 */
public class DltObservabilityRecoverer
        implements ConsumerAwareRecordRecoverer {

    /**
     * Registra o resultado definitivo do processamento
     * depois que todos os retries foram esgotados.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    DltObservabilityRecoverer.class
            );

    /**
     * Recoverer do Spring Kafka responsável por criar
     * e publicar a mensagem no tópico de DLT.
     */
    private final DeadLetterPublishingRecoverer delegate;

    /**
     * Componente responsável pelo contador
     * de mensagens enviadas com sucesso à DLT.
     */
    private final DltMetrics dltMetrics;

    /**
     * Recebe as dependências necessárias para recuperar
     * e observar uma mensagem definitivamente inválida.
     *
     * @param delegate recoverer responsável pelo envio real
     * @param dltMetrics métricas relacionadas à DLT
     */
    public DltObservabilityRecoverer(
            DeadLetterPublishingRecoverer delegate,
            DltMetrics dltMetrics
    ) {
        this.delegate = delegate;
        this.dltMetrics = dltMetrics;
    }

    /**
     * É chamado pelo tratamento de erros depois
     * que todas as tentativas foram esgotadas.
     *
     * @param registro mensagem original que falhou
     * @param consumer consumer que recebeu a mensagem
     * @param exception última falha do processamento
     */
    @Override
    public void accept(
            ConsumerRecord<?, ?> registro,
            Consumer<?, ?> consumer,
            Exception exception
    ) {
        /*
         * O recoverer também pode receber registros que falharam
         * durante a desserialização e não possuem nosso evento.
         *
         * Por isso, verificamos o tipo antes de acessar eventoId.
         */
        if (registro.value() instanceof PedidoCriadoEvent evento) {
            /*
             * O tratamento de erros acontece depois que o MDC
             * do consumer original já foi fechado.
             *
             * Reconstruímos novamente o contexto para os logs da DLT.
             */
            try (
                    MDC.MDCCloseable contextoEvento =
                            EventoLogContext.abrir(
                                    evento.eventoId()
                            )
            ) {
                recuperar(
                        registro,
                        consumer,
                        exception
                );
            }

            return;
        }

        /*
         * Se o payload não puder ser convertido para PedidoCriadoEvent,
         * ainda tentamos enviá-lo à DLT, mas sem eventoId no MDC.
         */
        recuperar(
                registro,
                consumer,
                exception
        );
    }

    /**
     * Delega o envio ao recoverer do Spring e registra
     * a observabilidade somente depois do sucesso.
     *
     * @param registro mensagem original
     * @param consumer consumer que recebeu a mensagem
     * @param exception falha que encerrou os retries
     */
    private void recuperar(
            ConsumerRecord<?, ?> registro,
            Consumer<?, ?> consumer,
            Exception exception
    ) {
        /*
         * O delegate publica a mensagem e espera a confirmação.
         *
         * Se o Kafka recusar ou não confirmar o envio,
         * este método lançará uma exceção e as linhas posteriores
         * não serão executadas.
         */
        delegate.accept(
                registro,
                consumer,
                exception
        );

        /*
         * Só incrementamos o Counter depois que o recoverer
         * concluiu o envio sem lançar uma exceção.
         */
        dltMetrics.registrarMensagemEnviada();

        /*
         * O tópico da DLT segue a convenção adotada pelo projeto:
         * nome do tópico original acrescido do sufixo "-dlt".
         */
        LOGGER.error(
                "Evento enviado para DLT: topico={}, particao={}, " +
                        "offsetOriginal={}, causa={}",
                registro.topic() + "-dlt",
                registro.partition(),
                registro.offset(),
                exception.getMessage()
        );
    }
}
