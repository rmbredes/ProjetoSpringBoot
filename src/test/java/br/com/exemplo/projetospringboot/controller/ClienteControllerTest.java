package br.com.exemplo.projetospringboot.controller;

import br.com.exemplo.projetospringboot.dto.ClienteDTO;
import br.com.exemplo.projetospringboot.service.ClienteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClienteService service;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void deveListarClientesParaUsuarioAutorizado() throws Exception {
        ClienteDTO cliente = new ClienteDTO(
                1L,
                "Ricardo",
                "ricardo@email.com",
                true
        );

        when(service.listar()).thenReturn(List.of(cliente));

        mockMvc.perform(get("/clientes")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_USER")
                        )))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nome").value("Ricardo"))
                .andExpect(jsonPath("$[0].email").value("ricardo@email.com"))
                .andExpect(jsonPath("$[0].ativo").value(true));
    }
}
