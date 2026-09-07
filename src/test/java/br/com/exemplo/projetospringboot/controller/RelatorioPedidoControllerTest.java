package br.com.exemplo.projetospringboot.controller;

import br.com.exemplo.projetospringboot.dto.RelatorioPedidoDTO;
import br.com.exemplo.projetospringboot.dto.UrlDownloadRelatorioDTO;
import br.com.exemplo.projetospringboot.enums.StatusRelatorioPedido;
import br.com.exemplo.projetospringboot.service.RelatorioPedidoService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa o contrato HTTP do controller de relatórios.
 *
 * <p>O SQS fica habilitado para que o controller exista no contexto,
 * mas seu cliente e o serviço de negócio são simulados. Nenhuma chamada
 * real é enviada para a AWS.</p>
 */
@SpringBootTest(
        properties = "application.aws.sqs.enabled=true"
)
@AutoConfigureMockMvc
class RelatorioPedidoControllerTest {

    /** Executa requisições HTTP simuladas contra a aplicação. */
    @Autowired
    private MockMvc mockMvc;

    /** Substitui o serviço real para testar apenas o controller. */
    @MockitoBean
    private RelatorioPedidoService service;

    /** Impede que o teste utilize o cliente real configurado para a AWS. */
    @MockitoBean
    private SqsClient sqsClient;

    /**
     * Confirma que uma solicitação autenticada responde HTTP 202.
     */
    @Test
    void deveAceitarSolicitacaoDeRelatorio() throws Exception {
        UUID solicitacaoId = UUID.randomUUID();

        RelatorioPedidoDTO resposta = new RelatorioPedidoDTO(
                15L,
                solicitacaoId,
                3L,
                StatusRelatorioPedido.ENFILEIRADO,
                null,
                null,
                null,
                null,
                0,
                null,
                Instant.parse("2026-09-03T20:00:00Z"),
                Instant.parse("2026-09-03T20:00:01Z"),
                null
        );

        when(service.solicitar(3L))
                .thenReturn(resposta);

        mockMvc.perform(
                        post("/pedidos/{pedidoId}/relatorios", 3L)
                                .with(
                                        jwt().authorities(
                                                new SimpleGrantedAuthority(
                                                        "ROLE_USER"
                                                )
                                        )
                                )
                )
                .andExpect(status().isAccepted())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                "application/json"
                        )
                )
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.pedidoId").value(3))
                .andExpect(
                        jsonPath("$.solicitacaoId")
                                .value(solicitacaoId.toString())
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("ENFILEIRADO")
                );
    }

    /**
     * Confirma que o endpoint continua protegido pela SecurityConfig.
     */
    @Test
    void naoDeveSolicitarRelatorioSemAutenticacao()
            throws Exception {
        mockMvc.perform(
                        post("/pedidos/{pedidoId}/relatorios", 3L)
                )
                .andExpect(status().isUnauthorized());
    }

    /**
     * Confirma que a repetição do POST devolve HTTP 200 quando o
     * relatório reutilizado já está disponível.
     */
    @Test
    void deveResponderOkAoReutilizarRelatorioDisponivel()
            throws Exception {
        RelatorioPedidoDTO existente = new RelatorioPedidoDTO(
                9L,
                UUID.randomUUID(),
                803L,
                StatusRelatorioPedido.DISPONIVEL,
                "relatorio-pedido-803.pdf",
                "application/pdf",
                1414L,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                        + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                0,
                null,
                Instant.parse("2026-09-04T01:28:13Z"),
                Instant.parse("2026-09-04T01:28:14Z"),
                Instant.parse("2026-09-04T01:28:14Z")
        );

        when(service.solicitar(803L))
                .thenReturn(existente);

        mockMvc.perform(
                        post("/pedidos/{pedidoId}/relatorios", 803L)
                                .with(
                                        jwt().authorities(
                                                new SimpleGrantedAuthority(
                                                        "ROLE_USER"
                                                )
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(
                        jsonPath("$.status")
                                .value("DISPONIVEL")
                );
    }

    /** Confirma o contrato HTTP da listagem de relatórios. */
    @Test
    void deveListarRelatoriosDoPedido() throws Exception {
        RelatorioPedidoDTO relatorio = new RelatorioPedidoDTO(
                2L,
                UUID.randomUUID(),
                800L,
                StatusRelatorioPedido.DISPONIVEL,
                "relatorio-pedido-800.pdf",
                "application/pdf",
                2500L,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                        + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                0,
                null,
                Instant.parse("2026-09-04T01:04:29Z"),
                Instant.parse("2026-09-04T01:04:31Z"),
                Instant.parse("2026-09-04T01:04:31Z")
        );

        when(service.listar(800L))
                .thenReturn(List.of(relatorio));

        mockMvc.perform(
                        get("/pedidos/{pedidoId}/relatorios", 800L)
                                .with(
                                        jwt().authorities(
                                                new SimpleGrantedAuthority(
                                                        "ROLE_USER"
                                                )
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2))
                .andExpect(
                        jsonPath("$[0].status")
                                .value("DISPONIVEL")
                );
    }

    /** Confirma o contrato da geração da URL assinada. */
    @Test
    void deveGerarUrlTemporariaDoRelatorio()
            throws Exception {
        when(service.gerarUrlDownload(800L, 2L))
                .thenReturn(
                        new UrlDownloadRelatorioDTO(
                                "https://s3.exemplo/relatorio-assinado",
                                Instant.parse("2026-09-04T01:10:00Z")
                        )
                );

        mockMvc.perform(
                        get(
                                "/pedidos/{pedidoId}/relatorios/{relatorioId}/download",
                                800L,
                                2L
                        )
                                .with(
                                        jwt().authorities(
                                                new SimpleGrantedAuthority(
                                                        "ROLE_USER"
                                                )
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.url")
                                .value(
                                        "https://s3.exemplo/relatorio-assinado"
                                )
                )
                .andExpect(
                        jsonPath("$.expiraEm")
                                .value("2026-09-04T01:10:00Z")
                );
    }
}
