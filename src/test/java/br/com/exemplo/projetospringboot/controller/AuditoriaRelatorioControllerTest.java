package br.com.exemplo.projetospringboot.controller;

import br.com.exemplo.projetospringboot.dto.AuditoriaRelatorioDTO;
import br.com.exemplo.projetospringboot.service.AuditoriaRelatorioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Testa o contrato HTTP da consulta de auditorias de relatório. */
@SpringBootTest(properties = "application.aws.sqs.enabled=true")
@AutoConfigureMockMvc
class AuditoriaRelatorioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditoriaRelatorioService service;

    /** Impede qualquer tentativa de comunicação real com o SQS. */
    @MockitoBean
    private SqsClient sqsClient;

    /** Confirma que um usuário autenticado pode consultar a auditoria. */
    @Test
    void deveListarAuditoriasDoPedido() throws Exception {
        UUID eventoId = UUID.randomUUID();

        when(service.listarPorPedido(808L))
                .thenReturn(
                        List.of(
                                new AuditoriaRelatorioDTO(
                                        1L,
                                        eventoId,
                                        "RELATORIO_PEDIDO_DISPONIVEL",
                                        14L,
                                        808L,
                                        UUID.randomUUID(),
                                        "pedidos/808/relatorios/arquivo.pdf",
                                        Instant.parse("2026-09-04T19:21:30Z"),
                                        Instant.parse("2026-09-04T19:21:31Z")
                                )
                        )
                );

        mockMvc.perform(
                        get(
                                "/pedidos/{pedidoId}/auditorias-relatorios",
                                808L
                        ).with(
                                jwt().authorities(
                                        new SimpleGrantedAuthority("ROLE_USER")
                                )
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventoId").value(eventoId.toString()))
                .andExpect(jsonPath("$[0].relatorioId").value(14))
                .andExpect(jsonPath("$[0].pedidoId").value(808));
    }

    /** Confirma que a consulta continua protegida pela autenticação. */
    @Test
    void naoDeveListarAuditoriasSemAutenticacao() throws Exception {
        mockMvc.perform(
                        get(
                                "/pedidos/{pedidoId}/auditorias-relatorios",
                                808L
                        )
                )
                .andExpect(status().isUnauthorized());
    }
}
