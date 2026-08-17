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

@WebMvcTest(ClienteController.class)
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteService service;

    @Test
    @WithMockUser(
            username = "ricardo",
            roles = "USER"
    )
    void deveListarClientes() throws Exception {

        ClienteDTO cliente =
                new ClienteDTO(
                        1L,
                        "Ricardo",
                        "ricardo@email.com",
                        true
                );

        when(
                service.listar()
        )
                .thenReturn(
                        List.of(cliente)
                );

        mockMvc.perform(
                        get("/clientes")
                )

                .andExpect(
                        status().isOk()
                )

                .andExpect(
                        content()
                                .contentTypeCompatibleWith(
                                        "application/json"
                                )
                )

                .andExpect(
                        jsonPath("$[0].id")
                                .value(1)
                )

                .andExpect(
                        jsonPath("$[0].nome")
                                .value("Ricardo")
                )

                .andExpect(
                        jsonPath("$[0].email")
                                .value(
                                        "ricardo@email.com"
                                )
                )

                .andExpect(
                        jsonPath("$[0].ativo")
                                .value(true)
                );
    }
}