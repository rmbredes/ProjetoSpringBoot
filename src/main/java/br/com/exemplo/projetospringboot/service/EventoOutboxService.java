package br.com.exemplo.projetospringboot.service;

import br.com.exemplo.projetospringboot.entity.EventoOutbox;
import br.com.exemplo.projetospringboot.event.PedidoCriadoEvent;
import br.com.exemplo.projetospringboot.repository.EventoOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Serviço responsável por registrar eventos na tabela de Outbox.
 *
 * Ele não publica diretamente no Kafka. Sua responsabilidade
 * é apenas preparar e salvar o evento no banco de dados.
 */
@Service
public class EventoOutboxService {

    /**
     * Nome do tópico que receberá os eventos de pedidos criados.
     */
    private static final String TOPICO_PEDIDOS_CRIADOS =
            "pedidos-criados";

    /**
     * Repositório utilizado para salvar os registros da Outbox.
     */
    private final EventoOutboxRepository eventoOutboxRepository;

    /**
     * Componente utilizado para converter objetos Java em JSON.
     */
    private final ObjectMapper objectMapper;

    /**
     * Recebe as dependências necessárias para registrar eventos.
     *
     * @param eventoOutboxRepository repositório da tabela de Outbox
     * @param objectMapper componente utilizado para gerar o JSON
     */
    public EventoOutboxService(
            EventoOutboxRepository eventoOutboxRepository,
            ObjectMapper objectMapper
    ) {
        this.eventoOutboxRepository = eventoOutboxRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Registra um evento de pedido criado na tabela de Outbox.
     *
     * A propagação MANDATORY exige que já exista uma transação aberta.
     * Dessa maneira, este método deverá ser chamado pela transação que
     * também está salvando o pedido.
     *
     * Se o pedido ou o evento falhar, toda a transação será desfeita.
     *
     * @param evento evento de pedido criado que será armazenado
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarPedidoCriado(
            PedidoCriadoEvent evento
    ) {
        String payload = converterParaJson(evento);

        EventoOutbox eventoOutbox = new EventoOutbox(
                evento.eventoId(),
                PedidoCriadoEvent.class.getSimpleName(),
                TOPICO_PEDIDOS_CRIADOS,
                evento.pedidoId().toString(),
                payload
        );

        eventoOutboxRepository.save(eventoOutbox);
    }

    /**
     * Converte o evento Java em uma representação JSON.
     *
     * Se a conversão falhar, uma exceção de execução será lançada.
     * Isso fará com que a transação atual seja desfeita, evitando
     * salvar o pedido sem o respectivo evento na Outbox.
     *
     * @param evento evento que será convertido
     * @return conteúdo JSON do evento
     */
    private String converterParaJson(
            PedidoCriadoEvent evento
    ) {
        try {
            return objectMapper.writeValueAsString(evento);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Não foi possível converter o evento de pedido para JSON",
                    exception
            );
        }
    }
}