package br.com.exemplo.projetospringboot.controller;

import br.com.exemplo.projetospringboot.dto.ClienteDTO;
import br.com.exemplo.projetospringboot.service.ClienteService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;


import org.springframework.security.test.context.support.WithMockUser;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testa o contrato HTTP do controlador de clientes isolado das demais camadas.
 */
@WebMvcTest(ClienteController.class)
class ClienteControllerTest {

    /** Cliente HTTP de teste que simula requisições ao controlador. */
    @Autowired
    private MockMvc mockMvc;

    /** Simulação do serviço para controlar o resultado devolvido ao controlador. */
    @MockitoBean
    private ClienteService service;

    /**
     * Confirma que um usuário autenticado recebe a lista em JSON e com status 200.
     */
    @Test
    @WithMockUser(
            username = "ricardo",
            roles = "USER"
    )
    void deveListarClientes() throws Exception {

        // Organiza o cliente que será devolvido pelo serviço simulado.
        ClienteDTO cliente =
                new ClienteDTO(
                        1L,
                        "Ricardo",
                        "ricardo@email.com",
                        true
                );

        // Programa o comportamento do serviço para este cenário.
        when(
                service.listar()
        )
                .thenReturn(
                        List.of(cliente)
                );

        // Executa a requisição GET contra o endpoint de clientes.
        mockMvc.perform(
                        get("/clientes")
                )

                // Confirma que a requisição foi atendida com sucesso.
                .andExpect(
                        status().isOk()
                )

                // Confirma que o corpo possui um tipo compatível com JSON.
                .andExpect(
                        content()
                                .contentTypeCompatibleWith(
                                        "application/json"
                                )
                )

                // Valida o identificador do primeiro item retornado.
                .andExpect(
                        jsonPath("$[0].id")
                                .value(1)
                )

                // Valida o nome do cliente retornado.
                .andExpect(
                        jsonPath("$[0].nome")
                                .value("Ricardo")
                )

                // Valida o e-mail do cliente retornado.
                .andExpect(
                        jsonPath("$[0].email")
                                .value(
                                        "ricardo@email.com"
                                )
                )

                // Valida a situação ativa do cliente.
                .andExpect(
                        jsonPath("$[0].ativo")
                                .value(true)
                );
    }
}
