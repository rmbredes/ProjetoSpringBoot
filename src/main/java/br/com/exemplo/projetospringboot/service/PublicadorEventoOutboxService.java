package br.com.exemplo.projetospringboot.service;

import br.com.exemplo.projetospringboot.entity.EventoOutbox;
import br.com.exemplo.projetospringboot.enums.StatusEventoOutbox;
import br.com.exemplo.projetospringboot.event.PedidoCriadoEvent;
import br.com.exemplo.projetospringboot.messaging.producer.PedidoProducer;
import br.com.exemplo.projetospringboot.repository.EventoOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Serviço responsável por publicar no Kafka um evento
 * que está armazenado na tabela de Outbox.
 */
@Service
public class PublicadorEventoOutboxService {

    /**
     * Quantidade máxima de tentativas automáticas de publicação.
     */
    private static final int LIMITE_TENTATIVAS = 5;

    /**
     * Componente utilizado para registrar informações e erros no log.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PublicadorEventoOutboxService.class);

    /**
     * Repositório utilizado para consultar e atualizar a Outbox.
     */
    private final EventoOutboxRepository eventoOutboxRepository;

    /**
     * Producer responsável pela publicação do evento no Kafka.
     */
    private final PedidoProducer pedidoProducer;

    /**
     * Componente utilizado para transformar o JSON armazenado
     * novamente em um objeto Java.
     */
    private final ObjectMapper objectMapper;

    /**
     * Recebe as dependências necessárias para publicar os eventos.
     *
     * @param eventoOutboxRepository repositório da Outbox
     * @param pedidoProducer producer responsável pelo Kafka
     * @param objectMapper componente responsável pelo JSON
     */
    public PublicadorEventoOutboxService(
            EventoOutboxRepository eventoOutboxRepository,
            PedidoProducer pedidoProducer,
            ObjectMapper objectMapper
    ) {
        this.eventoOutboxRepository = eventoOutboxRepository;
        this.pedidoProducer = pedidoProducer;
        this.objectMapper = objectMapper;
    }

    /**
     * Publica um evento pendente no Kafka.
     *
     * O registro é buscado novamente dentro desta transação.
     * Depois da confirmação do Kafka, ele é marcado como PUBLICADO.
     *
     * Se ocorrer uma falha, a quantidade de tentativas será
     * incrementada e o registro continuará disponível para retry.
     *
     * @param eventoOutboxId identificador do registro da Outbox
     */
    @Transactional
    public void publicar(
            Long eventoOutboxId
    ) {
        EventoOutbox eventoOutbox = eventoOutboxRepository
                .findById(eventoOutboxId)
                .orElseThrow();

        /*
         * Evita publicar novamente um evento que já tenha sido
         * concluído ou movido para o status ERRO.
         */
        if (eventoOutbox.getStatus() != StatusEventoOutbox.PENDENTE) {
            return;
        }

        try {
            PedidoCriadoEvent evento =
                    converterParaEvento(eventoOutbox.getPayload());

            /*
             * O método join aguarda a resposta do Kafka.
             *
             * Diferentemente do envio assíncrono anterior, somente
             * marcaremos o registro como PUBLICADO depois que o Kafka
             * confirmar que recebeu a mensagem.
             */
            pedidoProducer
                    .publicar(evento)
                    .join();

            eventoOutbox.marcarComoPublicado();

            LOGGER.info(
                    "Evento Outbox publicado: outboxId={}, eventoId={}",
                    eventoOutbox.getId(),
                    eventoOutbox.getEventoId()
            );
        } catch (Exception exception) {
            eventoOutbox.registrarFalha(
                    obterMensagemErro(exception),
                    LIMITE_TENTATIVAS
            );

            LOGGER.error(
                    "Falha ao publicar evento Outbox: outboxId={}, tentativa={}",
                    eventoOutbox.getId(),
                    eventoOutbox.getQuantidadeTentativas(),
                    exception
            );
        }

        /*
         * Não precisamos chamar save novamente.
         *
         * Como a entidade foi encontrada dentro desta transação,
         * o dirty checking do JPA identificará as alterações e
         * executará o UPDATE ao finalizar a transação.
         */
    }

    /**
     * Converte o JSON armazenado na Outbox novamente
     * para um evento de pedido criado.
     *
     * @param payload conteúdo JSON armazenado na Outbox
     * @return evento Java reconstruído
     * @throws JacksonException quando o JSON não puder ser convertido
     */
    private PedidoCriadoEvent converterParaEvento(
            String payload
    ) throws JacksonException {
        return objectMapper.readValue(
                payload,
                PedidoCriadoEvent.class
        );
    }

    /**
     * Obtém uma mensagem legível para registrar a falha no banco.
     *
     * @param exception falha ocorrida durante o processamento
     * @return mensagem que será armazenada na Outbox
     */
    private String obterMensagemErro(
            Exception exception
    ) {
        if (exception.getMessage() == null) {
            return exception.getClass().getSimpleName();
        }

        return exception.getMessage();
    }
}