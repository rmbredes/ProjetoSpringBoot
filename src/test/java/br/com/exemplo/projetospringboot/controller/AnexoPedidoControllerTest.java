package br.com.exemplo.projetospringboot.controller;

import br.com.exemplo.projetospringboot.dto.AnexoPedidoDTO;
import br.com.exemplo.projetospringboot.dto.UrlDownloadAnexoDTO;
import br.com.exemplo.projetospringboot.enums.StatusAnexoPedido;
import br.com.exemplo.projetospringboot.exception.ArquivoAnexoInvalidoException;
import br.com.exemplo.projetospringboot.service.AnexoPedidoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Carrega a aplicação completa para incluir a SecurityConfig real.
 *
 * <p>AutoConfigureMockMvc permite executar requisições HTTP simuladas
 * sem abrir uma porta de rede.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class AnexoPedidoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Substitui o serviço real dentro do contexto web.
     */
    @MockitoBean
    private AnexoPedidoService service;


    @Test
    void deveEnviarAnexo() throws Exception {
        MockMultipartFile arquivo =
                new MockMultipartFile(
                        "arquivo",
                        "nota-fiscal.pdf",
                        "application/pdf",
                        "conteúdo PDF simulado".getBytes()
                );

        AnexoPedidoDTO resposta =
                new AnexoPedidoDTO(
                        50L,
                        10L,
                        "nota-fiscal.pdf",
                        "application/pdf",
                        arquivo.getSize(),
                        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                        StatusAnexoPedido.DISPONIVEL,
                        Instant.parse(
                                "2026-09-02T12:00:00Z"
                        )
                );

        when(service.enviar(
                eq(10L),
                any()
        )).thenReturn(resposta);

        mockMvc.perform(
                        multipart(
                                "/pedidos/{pedidoId}/anexos",
                                10L
                        )
                                .file(arquivo)
                                /*
                                 * Simula um JWT já validado contendo a
                                 * autoridade exigida pela SecurityConfig.
                                 */
                                .with(
                                        jwt().authorities(
                                                new SimpleGrantedAuthority(
                                                        "ROLE_USER"
                                                )
                                        )
                                )
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        content()
                                .contentTypeCompatibleWith(
                                        "application/json"
                                )
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(50)
                )
                .andExpect(
                        jsonPath("$.pedidoId")
                                .value(10)
                )
                .andExpect(
                        jsonPath("$.nomeOriginal")
                                .value("nota-fiscal.pdf")
                )
                .andExpect(
                        jsonPath("$.contentType")
                                .value("application/pdf")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("DISPONIVEL")
                );
    }

    /**
     * Confirma que o endpoint exige autenticação.
     */
    @Test
    void naoDeveEnviarAnexoSemAutenticacao()
            throws Exception {

        MockMultipartFile arquivo =
                new MockMultipartFile(
                        "arquivo",
                        "arquivo.txt",
                        "text/plain",
                        "conteúdo".getBytes()
                );

        mockMvc.perform(
                        multipart(
                                "/pedidos/{pedidoId}/anexos",
                                10L
                        )
                                .file(arquivo)
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }


    @Test
    void deveResponderBadRequestParaArquivoInvalido()
            throws Exception {

        MockMultipartFile arquivo =
                new MockMultipartFile(
                        "arquivo",
                        "programa.exe",
                        "application/octet-stream",
                        "conteúdo".getBytes()
                );

        when(service.enviar(
                eq(10L),
                any()
        )).thenThrow(
                new ArquivoAnexoInvalidoException(
                        "Tipo de arquivo não permitido"
                )
        );

        mockMvc.perform(
                        multipart(
                                "/pedidos/{pedidoId}/anexos",
                                10L
                        )
                                .file(arquivo)
                                .with(
                                        jwt().authorities(
                                                new SimpleGrantedAuthority(
                                                        "ROLE_USER"
                                                )
                                        )
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.erro")
                                .value("Arquivo inválido")
                )
                .andExpect(
                        jsonPath("$.mensagem")
                                .value(
                                        "Tipo de arquivo não permitido"
                                )
                );
    }

    /**
     * Confirma o contrato HTTP da listagem de anexos.
     */
    @Test
    void deveListarAnexos() throws Exception {
        AnexoPedidoDTO anexo = criarRespostaAnexo();

        when(service.listar(10L))
                .thenReturn(List.of(anexo));

        mockMvc.perform(
                        get("/pedidos/{pedidoId}/anexos", 10L)
                                .with(
                                        jwt().authorities(
                                                new SimpleGrantedAuthority(
                                                        "ROLE_USER"
                                                )
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(50))
                .andExpect(
                        jsonPath("$[0].status")
                                .value("DISPONIVEL")
                );
    }

    /**
     * Confirma o contrato HTTP da geração de URL temporária.
     */
    @Test
    void deveGerarUrlTemporaria() throws Exception {
        when(service.gerarUrlDownload(10L, 50L))
                .thenReturn(
                        new UrlDownloadAnexoDTO(
                                "https://s3.exemplo/url-assinada",
                                Instant.parse(
                                        "2026-09-02T15:05:00Z"
                                )
                        )
                );

        mockMvc.perform(
                        get(
                                "/pedidos/{pedidoId}/anexos/{anexoId}/download",
                                10L,
                                50L
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
                                        "https://s3.exemplo/url-assinada"
                                )
                )
                .andExpect(
                        jsonPath("$.expiraEm")
                                .value("2026-09-02T15:05:00Z")
                );
    }

    /**
     * Confirma que a exclusão bem-sucedida responde sem corpo.
     */
    @Test
    void deveExcluirAnexo() throws Exception {
        mockMvc.perform(
                        delete(
                                "/pedidos/{pedidoId}/anexos/{anexoId}",
                                10L,
                                50L
                        )
                                .with(
                                        jwt().authorities(
                                                new SimpleGrantedAuthority(
                                                        "ROLE_ADMIN"
                                                )
                                        )
                                )
                )
                .andExpect(status().isNoContent());

        org.mockito.Mockito.verify(service)
                .excluir(10L, 50L);
    }

    /**
     * Cria uma resposta reutilizada nos testes de leitura.
     */
    private AnexoPedidoDTO criarRespostaAnexo() {
        return new AnexoPedidoDTO(
                50L,
                10L,
                "nota-fiscal.pdf",
                "application/pdf",
                100L,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
                        + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                StatusAnexoPedido.DISPONIVEL,
                Instant.parse("2026-09-02T12:00:00Z")
        );
    }
}
