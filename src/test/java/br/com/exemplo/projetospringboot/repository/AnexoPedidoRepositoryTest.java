package br.com.exemplo.projetospringboot.repository;

import br.com.exemplo.projetospringboot.ProjetoSpringBootApplication;
import br.com.exemplo.projetospringboot.entity.AnexoPedido;
import br.com.exemplo.projetospringboot.entity.Cliente;
import br.com.exemplo.projetospringboot.entity.Pedido;
import br.com.exemplo.projetospringboot.enums.StatusAnexoPedido;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testa as consultas do AnexoPedidoRepository utilizando PostgreSQL.
 *
 * <p>Este é um teste de integração de persistência. O Testcontainers
 * inicia um PostgreSQL descartável, o Spring cria as tabelas e utiliza
 * o repositório real do Spring Data JPA.</p>
 *
 * <p>A anotação Transactional desfaz as alterações ao final de cada
 * teste, mantendo um teste isolado dos demais.</p>
 */
@SpringBootTest(classes = ProjetoSpringBootApplication.class)
@Transactional
class AnexoPedidoRepositoryTest {

    /**
     * Hash fictício utilizado pelos registros de teste.
     */
    private static final String CHECKSUM_SHA256 =
            "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
                    + "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    @Autowired
    private AnexoPedidoRepository anexoPedidoRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    /**
     * Pedido persistido que será usado pelos testes.
     */
    private Pedido pedido;

    /**
     * Cria cliente e pedido antes de cada teste.
     */
    @BeforeEach
    void prepararDados() {
        /*
         * O UUID no e-mail impede colisão com registros que possam
         * ser criados por outros dados de teste.
         */
        Cliente cliente = new Cliente(
                null,
                "Cliente dos anexos",
                "anexos-" + UUID.randomUUID() + "@teste.local",
                true
        );

        Cliente clienteSalvo =
                clienteRepository.save(cliente);

        pedido = new Pedido(
                null,
                new BigDecimal("250.00"),
                LocalDateTime.now(),
                clienteSalvo
        );

        pedido = pedidoRepository.save(pedido);
    }

    /**
     * Confirma que a consulta filtra pelo pedido e pelo status.
     */
    @Test
    void deveListarSomenteAnexosDisponiveisDoPedido() {
        AnexoPedido disponivelUm = criarAnexo(
                "pedidos/" + pedido.getId() + "/anexos/um"
        );
        disponivelUm.marcarComoDisponivel("\"etag-um\"");

        AnexoPedido disponivelDois = criarAnexo(
                "pedidos/" + pedido.getId() + "/anexos/dois"
        );
        disponivelDois.marcarComoDisponivel("\"etag-dois\"");

        /*
         * Este anexo permanece PENDENTE e não deve aparecer
         * na consulta por DISPONIVEL.
         */
        AnexoPedido pendente = criarAnexo(
                "pedidos/" + pedido.getId() + "/anexos/pendente"
        );

        anexoPedidoRepository.save(disponivelUm);
        anexoPedidoRepository.save(disponivelDois);
        anexoPedidoRepository.save(pendente);

        List<AnexoPedido> resultado =
                anexoPedidoRepository
                        .findAllByPedidoIdAndStatusOrderByCriadoEmDesc(
                                pedido.getId(),
                                StatusAnexoPedido.DISPONIVEL
                        );

        assertThat(resultado)
                .hasSize(2)
                .allMatch(anexo ->
                        anexo.getStatus()
                                == StatusAnexoPedido.DISPONIVEL
                );

        assertThat(resultado)
                .extracting(AnexoPedido::getObjectKey)
                .containsExactlyInAnyOrder(
                        disponivelUm.getObjectKey(),
                        disponivelDois.getObjectKey()
                );
    }

    /**
     * Confirma que um anexo somente é encontrado dentro
     * do pedido ao qual realmente pertence.
     */
    @Test
    void deveBuscarAnexoPeloIdEPeloPedido() {
        AnexoPedido anexoSalvo =
                anexoPedidoRepository.save(
                        criarAnexo(
                                "pedidos/"
                                        + pedido.getId()
                                        + "/anexos/busca"
                        )
                );

        Optional<AnexoPedido> encontrado =
                anexoPedidoRepository.findByIdAndPedidoId(
                        anexoSalvo.getId(),
                        pedido.getId()
                );

        Optional<AnexoPedido> pedidoIncorreto =
                anexoPedidoRepository.findByIdAndPedidoId(
                        anexoSalvo.getId(),
                        999999L
                );

        assertThat(encontrado)
                .isPresent();

        assertThat(encontrado.orElseThrow().getObjectKey())
                .isEqualTo(anexoSalvo.getObjectKey());

        assertThat(pedidoIncorreto)
                .isEmpty();
    }

    /**
     * Confirma a consulta utilizada para proteger a unicidade da key.
     */
    @Test
    void deveIdentificarObjectKeyJaRegistrada() {
        String objectKey =
                "pedidos/" + pedido.getId() + "/anexos/unico";

        anexoPedidoRepository.save(
                criarAnexo(objectKey)
        );

        assertThat(
                anexoPedidoRepository.existsByObjectKey(objectKey)
        ).isTrue();

        assertThat(
                anexoPedidoRepository.existsByObjectKey(
                        "pedidos/"
                                + pedido.getId()
                                + "/anexos/inexistente"
                )
        ).isFalse();
    }

    /**
     * Cria metadados válidos associados ao pedido do teste.
     */
    private AnexoPedido criarAnexo(String objectKey) {
        return new AnexoPedido(
                pedido,
                objectKey,
                "arquivo-teste.txt",
                "text/plain",
                100L,
                CHECKSUM_SHA256
        );
    }
}