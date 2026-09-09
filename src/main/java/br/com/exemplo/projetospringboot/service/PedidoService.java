package br.com.exemplo.projetospringboot.service;

import br.com.exemplo.projetospringboot.dto.PedidoDTO;
import br.com.exemplo.projetospringboot.entity.Cliente;
import br.com.exemplo.projetospringboot.entity.Pedido;
import br.com.exemplo.projetospringboot.event.PedidoCriadoEvent;
import br.com.exemplo.projetospringboot.repository.ClienteRepository;
import br.com.exemplo.projetospringboot.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serviço responsável pelas regras de negócio relacionadas aos pedidos.
 */
@Service
public class PedidoService {

    /**
     * Logger utilizado para acompanhar as operações de pedidos.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PedidoService.class);

    /**
     * Registro central usado para criar observações.
     *
     * Cada observação poderá produzir métricas e spans.
     */
    private final ObservationRegistry observationRegistry;
    /**
     * Repositório utilizado para consultar os clientes.
     */
    private final ClienteRepository clienteRepository;

    /**
     * Repositório utilizado para salvar e consultar os pedidos.
     */
    private final PedidoRepository pedidoRepository;

    /**
     * Serviço utilizado para registrar eventos na tabela de Outbox.
     */
    private final EventoOutboxService eventoOutboxService;

    /**
     * Recebe as dependências necessárias para executar
     * as operações relacionadas aos pedidos.
     *
     * @param clienteRepository repositório de clientes
     * @param pedidoRepository repositório de pedidos
     * @param eventoOutboxService serviço responsável pela Outbox
     */
    public PedidoService(
            ObservationRegistry observationRegistry, ClienteRepository clienteRepository,
            PedidoRepository pedidoRepository,
            EventoOutboxService eventoOutboxService
    ) {
        this.observationRegistry = observationRegistry;
        this.clienteRepository = clienteRepository;
        this.pedidoRepository = pedidoRepository;
        this.eventoOutboxService = eventoOutboxService;
    }

    /**
     * Cria um pedido e registra seu evento na tabela de Outbox.
     *
     * O pedido e o evento são salvos dentro da mesma transação.
     * Portanto, ou os dois são confirmados ou os dois são desfeitos.
     *
     * @param pedidoDTO dados recebidos para criação do pedido
     * @return dados do pedido criado
     */
    @Transactional
    public PedidoDTO criarPedido(
            PedidoDTO pedidoDTO
    ) {
        return Observation
                .createNotStarted(
                        "pedido.criar",
                        observationRegistry
                )
                .observe(
                        () -> executarCriacao(pedidoDTO)
                );
    }
    /**
     * Executa a criação do pedido dentro da observação iniciada
     * pelo método público.
     *
     * @param pedidoDTO dados recebidos para criação
     * @return pedido criado
     */
    private PedidoDTO executarCriacao(
            PedidoDTO pedidoDTO
    ) {
        Cliente cliente = clienteRepository
                .findById(pedidoDTO.clienteId())
                .orElseThrow();

        Pedido pedido = new Pedido();

        pedido.setCliente(cliente);
        pedido.setValor(pedidoDTO.valor());
        pedido.setDataCriacao(LocalDateTime.now());

        Pedido pedidoSalvo =
                pedidoRepository.save(pedido);

        PedidoCriadoEvent evento =
                new PedidoCriadoEvent(
                        UUID.randomUUID(),
                        pedidoSalvo.getId(),
                        cliente.getId(),
                        pedidoSalvo.getValor(),
                        Instant.now()
                );

        eventoOutboxService.registrarPedidoCriado(evento);

        LOGGER.info(
                "Pedido persistido com evento na Outbox: pedidoId={}",
                pedidoSalvo.getId()
        );

        return converterParaDTO(pedidoSalvo);
    }
    /**
     * Lista todos os pedidos cadastrados.
     *
     * @return lista de pedidos convertidos para DTO
     */
    public List<PedidoDTO> listar() {

        return Observation
                .createNotStarted(
                        "pedido.listar",
                        observationRegistry
                )
                .observe(() -> {

                    List<PedidoDTO> pedidos =
                            pedidoRepository.findAll()
                                    .stream()
                                    .map(this::converterParaDTO)
                                    .toList();

                    LOGGER.info(
                            "Pedidos consultados: quantidade={}",
                            pedidos.size()
                    );

                    return pedidos;
                });
    }

    /**
     * Busca um pedido pelo seu identificador.
     *
     * @param id identificador do pedido
     * @return pedido encontrado convertido para DTO
     */
    public PedidoDTO buscar(
            Long id
    ) {
        Optional<Pedido> pedidoOpcional =
                pedidoRepository.findById(id);

        Pedido pedido = pedidoOpcional.orElseThrow();

        return converterParaDTO(pedido);
    }

    /**
     * Lista todos os pedidos pertencentes a determinado cliente.
     *
     * @param id identificador do cliente
     * @return pedidos encontrados convertidos para DTO
     */
    public List<PedidoDTO> listaPorCLiente(
            Long id
    ) {
        return pedidoRepository.findByClienteId(id)
                .stream()
                .map(this::converterParaDTO)
                .toList();
    }

    /**
     * Exclui um pedido pelo seu identificador.
     *
     * @param idPedido identificador do pedido que será excluído
     */
    @Transactional
    public void excluir(
            Long idPedido
    ) {
        Pedido pedido = buscarPedido(idPedido);

        pedidoRepository.delete(pedido);
    }

    /**
     * Busca internamente a entidade Pedido pelo identificador.
     *
     * @param idPedido identificador do pedido
     * @return entidade encontrada
     */
    private Pedido buscarPedido(
            Long idPedido
    ) {
        Optional<Pedido> pedidoOpcional =
                pedidoRepository.findById(idPedido);

        return pedidoOpcional.orElseThrow();
    }

    /**
     * Converte uma entidade Pedido para PedidoDTO.
     *
     * @param pedido entidade que será convertida
     * @return representação DTO do pedido
     */
    private PedidoDTO converterParaDTO(
            Pedido pedido
    ) {
        return new PedidoDTO(
                pedido.getId(),
                pedido.getValor(),
                pedido.getDataCriacao(),
                pedido.getCliente().getId(),
                pedido.getPagamentoEventoId(),
                pedido.getPagamentoId(),
                pedido.getStatusPagamento(),
                pedido.getStatusLogistica()
        );
    }
}