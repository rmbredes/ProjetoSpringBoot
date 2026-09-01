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

/**
 * Testa as regras do serviço de clientes sem acessar um banco de dados real.
 */
@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    /** Repositório simulado para controlar consultas e persistências. */
    @Mock
    private ClienteRepository repository;

    /** Serviço real que recebe automaticamente o repositório simulado. */
    @InjectMocks
    private ClienteService service;

    /** Cliente padrão reutilizado nos cenários de teste. */
    private Cliente cliente;

    /** Prepara uma entidade válida antes da execução de cada teste. */
    @BeforeEach
    void prepararDados() {

        // Cria um novo objeto para impedir que um teste reaproveite alterações de outro.
        cliente = new Cliente();

        // Preenche os valores que representam um cliente já persistido.
        cliente.setId(1L);
        cliente.setNome("Ricardo");
        cliente.setEmail("ricardo@email.com");
        cliente.setAtivo(true);
    }

    /** Confirma a busca e a conversão de uma entidade existente para DTO. */
    @Test
    void deveBuscarClientePorId() {

        // Organiza o retorno da consulta simulada.
        when(
                repository.findById(1L)
        )
                .thenReturn(
                        Optional.of(cliente)
                );

        // Executa o método que está sendo testado.
        ClienteDTO resultado =
                service.buscar(1L);

        // Confirma que o serviço devolveu um objeto.
        assertNotNull(resultado);

        // Confirma que todos os campos foram convertidos corretamente.
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

        // Garante que a consulta ocorreu uma única vez.
        verify(
                repository,
                times(1)
        )
                .findById(1L);
    }

    /** Confirma que todas as entidades retornadas são convertidas para DTO. */
    @Test
    void deveListarClientes() {

        // Organiza um segundo cliente para validar uma lista com vários elementos.
        Cliente cliente2 =
                new Cliente();

        // Preenche os dados particulares do segundo cliente.
        cliente2.setId(2L);
        cliente2.setNome("João");
        cliente2.setEmail("joao@email.com");
        cliente2.setAtivo(true);

        // Programa o repositório para devolver os dois clientes.
        when(
                repository.findAll()
        )
                .thenReturn(
                        List.of(
                                cliente,
                                cliente2
                        )
                );

        // Executa a listagem do serviço.
        List<ClienteDTO> resultado =
                service.listar();

        // Confirma a existência e a quantidade de resultados.
        assertNotNull(resultado);

        assertEquals(
                2,
                resultado.size()
        );

        // Confirma a conversão e a ordem dos nomes retornados.
        assertEquals(
                "Ricardo",
                resultado.get(0).nome()
        );

        assertEquals(
                "João",
                resultado.get(1).nome()
        );

        // Garante que a consulta de todos os clientes ocorreu uma vez.
        verify(
                repository,
                times(1)
        )
                .findAll();
    }

    /** Confirma que a listagem utiliza a consulta destinada aos clientes ativos. */
    @Test
    void deveListarClientesAtivos() {

        // Organiza o único cliente ativo devolvido pelo repositório.
        when(
                repository.findByAtivoTrue()
        )
                .thenReturn(
                        List.of(cliente)
                );

        // Executa a listagem filtrada.
        List<ClienteDTO> resultado =
                service.listarAtivos();

        // Confirma a quantidade e a situação do item retornado.
        assertNotNull(resultado);

        assertEquals(
                1,
                resultado.size()
        );

        assertTrue(
                resultado.get(0).ativo()
        );

        // Garante o uso da consulta específica para registros ativos.
        verify(
                repository,
                times(1)
        )
                .findByAtivoTrue();
    }

    /** Confirma a criação, a persistência e a conversão do novo cliente. */
    @Test
    void deveCriarCliente() {

        // Organiza os dados de entrada ainda sem identificador.
        ClienteDTO entrada =
                new ClienteDTO(
                        null,
                        "Ricardo",
                        "ricardo@email.com",
                        true
                );

        // Simula o banco atribuindo um identificador à entidade salva.
        when(
                repository.save(
                        any(Cliente.class)
                )
        )
                .thenAnswer(
                        invocation -> {

                            // Recupera a própria entidade entregue ao método save.
                            Cliente clienteSalvo =
                                    invocation.getArgument(0);

                            // Representa o identificador gerado pelo banco.
                            clienteSalvo.setId(1L);

                            // Devolve a entidade como faria o repositório real.
                            return clienteSalvo;
                        }
                );

        // Executa a criação do cliente.
        ClienteDTO resultado =
                service.criar(
                        entrada
                );

        // Confirma que o resultado foi criado e recebeu o identificador simulado.
        assertNotNull(resultado);

        assertEquals(
                1L,
                resultado.id()
        );

        // Confirma que nome e e-mail foram preservados.
        assertEquals(
                "Ricardo",
                resultado.nome()
        );

        assertEquals(
                "ricardo@email.com",
                resultado.email()
        );

        // Garante que uma entidade de cliente foi enviada para persistência.
        verify(
                repository,
                times(1)
        )
                .save(
                        any(Cliente.class)
                );
    }

    /** Confirma que todos os campos editáveis de um cliente são atualizados. */
    @Test
    void deveAtualizarCliente() {

        // Organiza os novos valores que substituirão os dados atuais.
        ClienteDTO entrada =
                new ClienteDTO(
                        null,
                        "Ricardo Atualizado",
                        "novo@email.com",
                        false
                );

        // Simula a localização da entidade existente.
        when(
                repository.findById(1L)
        )
                .thenReturn(
                        Optional.of(cliente)
                );

        // Simula a persistência devolvendo a mesma entidade já modificada.
        when(
                repository.save(
                        any(Cliente.class)
                )
        )
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0)
                );

        // Executa a atualização.
        ClienteDTO resultado =
                service.atualizar(
                        1L,
                        entrada
                );

        // Confirma os novos valores devolvidos pelo serviço.
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

        // Confirma que o cliente foi consultado e depois salvo.
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

    /** Confirma que a operação específica modifica somente o e-mail. */
    @Test
    void deveAlterarEmail() {

        // Simula a localização do cliente que será alterado.
        when(
                repository.findById(1L)
        )
                .thenReturn(
                        Optional.of(cliente)
                );

        // Simula a persistência da entidade modificada.
        when(
                repository.save(
                        cliente
                )
        )
                .thenReturn(
                        cliente
                );

        // Executa a alteração do e-mail.
        ClienteDTO resultado =
                service.alterarEmail(
                        1L,
                        "emailnovo@email.com"
                );

        // Confirma que o novo e-mail foi devolvido.
        assertEquals(
                "emailnovo@email.com",
                resultado.email()
        );

        // Confirma a consulta e a persistência da entidade.
        verify(
                repository
        )
                .findById(1L);

        verify(
                repository
        )
                .save(cliente);
    }

    /** Confirma que um cliente existente é localizado e excluído. */
    @Test
    void deveExcluirCliente() {

        // Organiza o retorno da busca obrigatória antes da exclusão.
        when(
                repository.findById(1L)
        )
                .thenReturn(
                        Optional.of(cliente)
                );

        // Executa a exclusão.
        service.excluir(1L);

        // Confirma que o serviço consultou e removeu a entidade correta.
        verify(
                repository
        )
                .findById(1L);

        verify(
                repository
        )
                .delete(cliente);
    }

    /** Confirma que a busca de um identificador inexistente lança uma exceção. */
    @Test
    void deveLancarExcecaoQuandoClienteNaoExistir() {

        // Simula uma consulta sem resultado.
        when(
                repository.findById(999L)
        )
                .thenReturn(
                        Optional.empty()
                );

        // Executa a busca e confirma a exceção esperada.
        assertThrows(
                java.util.NoSuchElementException.class,
                () ->
                        service.buscar(999L)
        );

        // Garante que a consulta foi realizada com o identificador do cenário.
        verify(
                repository
        )
                .findById(999L);
    }
}
