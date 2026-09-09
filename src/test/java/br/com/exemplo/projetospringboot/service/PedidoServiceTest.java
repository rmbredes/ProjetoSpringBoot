package br.com.exemplo.projetospringboot.service;
import br.com.exemplo.projetospringboot.dto.PedidoDTO;
import br.com.exemplo.projetospringboot.entity.Cliente;
import br.com.exemplo.projetospringboot.entity.Pedido;
import br.com.exemplo.projetospringboot.repository.ClienteRepository;
import br.com.exemplo.projetospringboot.repository.PedidoRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.exemplo.projetospringboot.event.PedidoCriadoEvent;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testa as regras de pedidos e a criação do evento de domínio sem dependências reais.
 */
@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {
    /** Repositório simulado para localizar o cliente do pedido. */
    @Mock
    private ClienteRepository clienteRepository;

    /** Repositório simulado para controlar as operações de pedidos. */
    @Mock
    private PedidoRepository pedidoRepository;

    /*
     * Simula o serviço de Outbox para que este teste verifique
     * somente a responsabilidade do PedidoService.
     */
    @Mock
    private EventoOutboxService eventoOutboxService;

    /** Serviço real que recebe automaticamente as dependências simuladas. */
    @InjectMocks
    private PedidoService pedidoService;

    /** Cliente padrão usado nos cenários. */
    private Cliente cliente;

    /** Pedido padrão usado nos cenários de consulta e exclusão. */
    private Pedido pedido;

    /** Prepara entidades válidas e independentes antes de cada teste. */
    @BeforeEach
    void prepararDados() {

        // Cria o cliente que será associado ao pedido.
        cliente =
                new Cliente(
                        1L,
                        "Ricardo",
                        "ricardo@email.com",
                        true
                );

        // Cria um pedido que representa um registro já persistido.
        pedido =
                new Pedido(
                        10L,
                        new BigDecimal("150.00"),
                        LocalDateTime.of(
                                2026,
                                8,
                                16,
                                10,
                                30
                        ),
                        cliente
                );
    }

    /** Confirma que pedido e evento de domínio são criados no mesmo fluxo. */
    @Test
    void deveCriarPedido() {

        // Organiza os dados recebidos para a criação.
        PedidoDTO entrada =
                new PedidoDTO(
                        null,
                        new BigDecimal("150.00"),
                        null,
                        1L,
                null,
                null,
                null,
                        null
                );

        // Simula a localização do cliente informado no pedido.
        when(
                clienteRepository.findById(1L)
        )
                .thenReturn(
                        Optional.of(cliente)
                );

        // Simula a persistência atribuindo o identificador gerado pelo banco.
        when(
                pedidoRepository.save(
                        any(Pedido.class)
                )
        )
                .thenAnswer(
                        invocation -> {

                            // Recupera a entidade enviada ao repositório.
                            Pedido pedidoSalvo =
                                    invocation.getArgument(0);

                            // Representa o identificador atribuído pelo banco.
                            pedidoSalvo.setId(10L);

                            // Devolve a entidade persistida ao serviço.
                            return pedidoSalvo;
                        }
                );

        PedidoDTO resultado =
                pedidoService.criarPedido(
                        entrada
                );

        assertNotNull(resultado);

        assertEquals(
                10L,
                resultado.id()
        );

        assertEquals(
                new BigDecimal("150.00"),
                resultado.valor()
        );

        assertEquals(
                1L,
                resultado.clienteId()
        );

        assertNotNull(
                resultado.dataCriacao()
        );

        verify(
                clienteRepository
        )
                .findById(1L);

        verify(
                pedidoRepository
        )
                .save(
                        any(Pedido.class)
                );


        /*
         * Captura o evento entregue ao serviço de Outbox para verificar
         * se o PedidoService montou corretamente os dados do evento.
         */
        ArgumentCaptor<PedidoCriadoEvent> eventoCaptor =
                ArgumentCaptor.forClass(PedidoCriadoEvent.class);

        verify(
                eventoOutboxService
        ).registrarPedidoCriado(
                eventoCaptor.capture()
        );

        /*
         * Recupera o evento capturado para validar seus atributos.
         */
        PedidoCriadoEvent eventoRegistrado =
                eventoCaptor.getValue();

        assertNotNull(
                eventoRegistrado.eventoId()
        );

        assertEquals(
                10L,
                eventoRegistrado.pedidoId()
        );

        assertEquals(
                1L,
                eventoRegistrado.clienteId()
        );

        assertEquals(
                new BigDecimal("150.00"),
                eventoRegistrado.valor()
        );

        assertNotNull(
                eventoRegistrado.ocorridoEm()
        );
    }

    /** Confirma que todos os pedidos encontrados são convertidos para DTO. */
    @Test
    void deveListarPedidos() {

        when(
                pedidoRepository.findAll()
        )
                .thenReturn(
                        List.of(pedido)
                );

        List<PedidoDTO> resultado =
                pedidoService.listar();

        assertEquals(
                1,
                resultado.size()
        );

        assertEquals(
                10L,
                resultado.get(0).id()
        );

        assertEquals(
                1L,
                resultado.get(0).clienteId()
        );

        verify(
                pedidoRepository
        )
                .findAll();
    }

    /** Confirma a busca e a conversão de um pedido pelo identificador. */
    @Test
    void deveBuscarPedidoPorId() {

        when(
                pedidoRepository.findById(10L)
        )
                .thenReturn(
                        Optional.of(pedido)
                );

        PedidoDTO resultado =
                pedidoService.buscar(10L);

        assertNotNull(resultado);

        assertEquals(
                10L,
                resultado.id()
        );

        assertEquals(
                new BigDecimal("150.00"),
                resultado.valor()
        );

        verify(
                pedidoRepository
        )
                .findById(10L);
    }

    /** Confirma o uso da consulta que filtra os pedidos por cliente. */
    @Test
    void deveListarPedidosPorCliente() {

        when(
                pedidoRepository.findByClienteId(1L)
        )
                .thenReturn(
                        List.of(pedido)
                );

        List<PedidoDTO> resultado =
                pedidoService.listaPorCLiente(1L);

        assertEquals(
                1,
                resultado.size()
        );

        assertEquals(
                1L,
                resultado.get(0).clienteId()
        );

        verify(
                pedidoRepository
        )
                .findByClienteId(1L);
    }

    /** Confirma que um pedido existente é localizado e excluído. */
    @Test
    void deveExcluirPedido() {

        when(
                pedidoRepository.findById(10L)
        )
                .thenReturn(
                        Optional.of(pedido)
                );

        pedidoService.excluir(10L);

        verify(
                pedidoRepository
        )
                .findById(10L);

        verify(
                pedidoRepository
        )
                .delete(pedido);
    }

    /** Confirma que a ausência do pedido interrompe a busca com uma exceção. */
    @Test
    void deveLancarExcecaoQuandoPedidoNaoExistir() {

        when(
                pedidoRepository.findById(999L)
        )
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                java.util.NoSuchElementException.class,
                () ->
                        pedidoService.buscar(999L)
        );
    }
}
