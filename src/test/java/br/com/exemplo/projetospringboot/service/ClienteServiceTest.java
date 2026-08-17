package br.com.exemplo.projetospringboot.service;

import br.com.exemplo.projetospringboot.dto.ClienteDTO;
import br.com.exemplo.projetospringboot.entity.Cliente;
import br.com.exemplo.projetospringboot.repository.ClienteRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository repository;

    @InjectMocks
    private ClienteService service;

    private Cliente cliente;

    @BeforeEach
    void prepararDados() {

        cliente = new Cliente();

        cliente.setId(1L);
        cliente.setNome("Ricardo");
        cliente.setEmail("ricardo@email.com");
        cliente.setAtivo(true);
    }

    @Test
    void deveBuscarClientePorId() {

        when(
                repository.findById(1L)
        )
                .thenReturn(
                        Optional.of(cliente)
                );

        ClienteDTO resultado =
                service.buscar(1L);

        assertNotNull(resultado);

        assertEquals(
                1L,
                resultado.id()
        );

        assertEquals(
                "Ricardo",
                resultado.nome()
        );

        assertEquals(
                "ricardo@email.com",
                resultado.email()
        );

        assertTrue(
                resultado.ativo()
        );

        verify(
                repository,
                times(1)
        )
                .findById(1L);
    }

    @Test
    void deveListarClientes() {

        Cliente cliente2 =
                new Cliente();

        cliente2.setId(2L);
        cliente2.setNome("João");
        cliente2.setEmail("joao@email.com");
        cliente2.setAtivo(true);

        when(
                repository.findAll()
        )
                .thenReturn(
                        List.of(
                                cliente,
                                cliente2
                        )
                );

        List<ClienteDTO> resultado =
                service.listar();

        assertNotNull(resultado);

        assertEquals(
                2,
                resultado.size()
        );

        assertEquals(
                "Ricardo",
                resultado.get(0).nome()
        );

        assertEquals(
                "João",
                resultado.get(1).nome()
        );

        verify(
                repository,
                times(1)
        )
                .findAll();
    }

    @Test
    void deveListarClientesAtivos() {

        when(
                repository.findByAtivoTrue()
        )
                .thenReturn(
                        List.of(cliente)
                );

        List<ClienteDTO> resultado =
                service.listarAtivos();

        assertNotNull(resultado);

        assertEquals(
                1,
                resultado.size()
        );

        assertTrue(
                resultado.get(0).ativo()
        );

        verify(
                repository,
                times(1)
        )
                .findByAtivoTrue();
    }

    @Test
    void deveCriarCliente() {

        ClienteDTO entrada =
                new ClienteDTO(
                        null,
                        "Ricardo",
                        "ricardo@email.com",
                        true
                );

        when(
                repository.save(
                        any(Cliente.class)
                )
        )
                .thenAnswer(
                        invocation -> {

                            Cliente clienteSalvo =
                                    invocation.getArgument(0);

                            clienteSalvo.setId(1L);

                            return clienteSalvo;
                        }
                );

        ClienteDTO resultado =
                service.criar(
                        entrada
                );

        assertNotNull(resultado);

        assertEquals(
                1L,
                resultado.id()
        );

        assertEquals(
                "Ricardo",
                resultado.nome()
        );

        assertEquals(
                "ricardo@email.com",
                resultado.email()
        );

        verify(
                repository,
                times(1)
        )
                .save(
                        any(Cliente.class)
                );
    }

    @Test
    void deveAtualizarCliente() {

        ClienteDTO entrada =
                new ClienteDTO(
                        null,
                        "Ricardo Atualizado",
                        "novo@email.com",
                        false
                );

        when(
                repository.findById(1L)
        )
                .thenReturn(
                        Optional.of(cliente)
                );

        when(
                repository.save(
                        any(Cliente.class)
                )
        )
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0)
                );

        ClienteDTO resultado =
                service.atualizar(
                        1L,
                        entrada
                );

        assertEquals(
                "Ricardo Atualizado",
                resultado.nome()
        );

        assertEquals(
                "novo@email.com",
                resultado.email()
        );

        assertFalse(
                resultado.ativo()
        );

        verify(
                repository
        )
                .findById(1L);

        verify(
                repository
        )
                .save(
                        cliente
                );
    }

    @Test
    void deveAlterarEmail() {

        when(
                repository.findById(1L)
        )
                .thenReturn(
                        Optional.of(cliente)
                );

        when(
                repository.save(
                        cliente
                )
        )
                .thenReturn(
                        cliente
                );

        ClienteDTO resultado =
                service.alterarEmail(
                        1L,
                        "emailnovo@email.com"
                );

        assertEquals(
                "emailnovo@email.com",
                resultado.email()
        );

        verify(
                repository
        )
                .findById(1L);

        verify(
                repository
        )
                .save(cliente);
    }

    @Test
    void deveExcluirCliente() {

        when(
                repository.findById(1L)
        )
                .thenReturn(
                        Optional.of(cliente)
                );

        service.excluir(1L);

        verify(
                repository
        )
                .findById(1L);

        verify(
                repository
        )
                .delete(cliente);
    }

    @Test
    void deveLancarExcecaoQuandoClienteNaoExistir() {

        when(
                repository.findById(999L)
        )
                .thenReturn(
                        Optional.empty()
                );

        assertThrows(
                java.util.NoSuchElementException.class,
                () ->
                        service.buscar(999L)
        );

        verify(
                repository
        )
                .findById(999L);
    }
}