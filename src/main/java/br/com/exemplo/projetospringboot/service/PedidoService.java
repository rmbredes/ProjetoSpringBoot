package br.com.exemplo.projetospringboot.service;

import br.com.exemplo.projetospringboot.dto.ClienteDTO;
import br.com.exemplo.projetospringboot.dto.PedidoDTO;
import br.com.exemplo.projetospringboot.entity.Cliente;
import br.com.exemplo.projetospringboot.entity.Pedido;
import br.com.exemplo.projetospringboot.repository.ClienteRepository;
import br.com.exemplo.projetospringboot.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PedidoService {
    private final ClienteRepository clienteRepository;
    private final PedidoRepository  pedidoRepository;

    public PedidoService(ClienteRepository clienteRepository, PedidoRepository pedidoRepository) {
        this.clienteRepository = clienteRepository;
        this.pedidoRepository = pedidoRepository;
    }
    @Transactional
    public PedidoDTO criarPedido(
            PedidoDTO pedidoDTO
    ) {

        Optional<Cliente> clienteOptional =
                clienteRepository.findById(
                        pedidoDTO.clienteId()
                );

        Cliente cliente =
                clienteOptional.orElseThrow();

        Pedido pedido = new Pedido();

        pedido.setCliente(cliente);
        pedido.setValor(pedidoDTO.valor());
        pedido.setDataCriacao(
                LocalDateTime.now()
        );

        return converterParaDTO(pedidoRepository.save(
                pedido)
        );
    }

    public List<PedidoDTO> listar(){
        return pedidoRepository.findAll()
                .stream()
                .map(this::converterParaDTO)
                .toList();

    }

    public PedidoDTO buscar(Long id){

        Optional<Pedido> pedidoOpcional = pedidoRepository.findById(id);

        Pedido pedido = pedidoOpcional.orElseThrow();

        return converterParaDTO(pedido);
    }

    public List<PedidoDTO> listaPorCLiente(Long id){
        return pedidoRepository.findByClienteId(id)
                .stream()
                .map(this::converterParaDTO)
                .toList();
    }

    @Transactional
    public void excluir(Long idPedido){

        Pedido pedido=buscarPedido(idPedido);

        pedidoRepository.delete(pedido);
    }

    private Pedido buscarPedido(Long idPedido){
        Optional<Pedido> pedidoOpcional = pedidoRepository.findById(idPedido);

        return pedidoOpcional.orElseThrow();
    }
    private PedidoDTO converterParaDTO(Pedido pedido) {

        return new PedidoDTO(
                pedido.getId(),
                pedido.getValor(),
                pedido.getDataCriacao(),
                pedido.getCliente().getId()
        );


    }

}
