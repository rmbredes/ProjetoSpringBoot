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

/**
 * Serviço responsável pelas regras de negócio relacionadas aos pedidos.
 */
@Service
public class PedidoService {

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
            ClienteRepository clienteRepository,
            PedidoRepository pedidoRepository,
            EventoOutboxService eventoOutboxService
    ) {
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
        /*
         * Procura o cliente informado no pedido.
         */
        Cliente cliente = clienteRepository
                .findById(pedidoDTO.clienteId())
                .orElseThrow();

        /*
         * Monta a entidade que será salva no banco.
         */
        Pedido pedido = new Pedido();

        pedido.setCliente(cliente);
        pedido.setValor(pedidoDTO.valor());
        pedido.setDataCriacao(LocalDateTime.now());

        /*
         * Salva primeiro para que o banco gere o pedidoId.
         *
         * A confirmação definitiva ainda não aconteceu.
         * Ela ocorrerá somente no final da transação.
         */
        Pedido pedidoSalvo = pedidoRepository.save(pedido);

        /*
         * Cria a fotografia imutável do pedido no momento
         * em que ele foi criado.
         */
        PedidoCriadoEvent evento = new PedidoCriadoEvent(
                UUID.randomUUID(),
                pedidoSalvo.getId(),
                cliente.getId(),
                pedidoSalvo.getValor(),
                Instant.now()
        );

        /*
         * Registra o evento na tabela de Outbox.
         *
         * Esse método participa da mesma transação utilizada
         * para salvar o pedido.
         */
        eventoOutboxService.registrarPedidoCriado(evento);

        /*
         * Converte e retorna os dados do pedido salvo.
         */
        return converterParaDTO(pedidoSalvo);
    }

    /**
     * Lista todos os pedidos cadastrados.
     *
     * @return lista de pedidos convertidos para DTO
     */
    public List<PedidoDTO> listar() {
        return pedidoRepository.findAll()
                .stream()
                .map(this::converterParaDTO)
                .toList();
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
                pedido.getCliente().getId()
        );
    }
}