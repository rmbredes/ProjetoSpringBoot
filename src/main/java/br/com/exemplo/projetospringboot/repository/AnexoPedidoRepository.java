package br.com.exemplo.projetospringboot.repository;

import br.com.exemplo.projetospringboot.entity.AnexoPedido;
import br.com.exemplo.projetospringboot.enums.StatusAnexoPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Fornece as operações de persistência dos anexos de pedidos.
 *
 * <p>JpaRepository já fornece métodos como:</p>
 *
 * <ul>
 *     <li>save;</li>
 *     <li>findById;</li>
 *     <li>findAll;</li>
 *     <li>delete;</li>
 *     <li>count.</li>
 * </ul>
 *
 * <p>Os métodos adicionais abaixo são interpretados automaticamente
 * pelo Spring Data JPA a partir de seus nomes.</p>
 */
public interface AnexoPedidoRepository
        extends JpaRepository<AnexoPedido, Long> {

    /**
     * Lista os anexos de um pedido que estejam no status informado.
     *
     * <p>A ordenação mais recente primeiro é útil para a resposta
     * da futura API.</p>
     *
     * @param pedidoId identificador do pedido
     * @param status estado que será utilizado como filtro
     * @return anexos encontrados, do mais recente para o mais antigo
     */
    List<AnexoPedido> findAllByPedidoIdAndStatusOrderByCriadoEmDesc(
            Long pedidoId,
            StatusAnexoPedido status
    );

    /**
     * Procura um anexo por seu identificador e pelo pedido proprietário.
     *
     * <p>Incluir o pedido na consulta evita manipular acidentalmente
     * um anexo utilizando o endereço de outro pedido.</p>
     *
     * @param id identificador do anexo
     * @param pedidoId identificador do pedido
     * @return anexo encontrado ou Optional vazio
     */
    Optional<AnexoPedido> findByIdAndPedidoId(
            Long id,
            Long pedidoId
    );

    /**
     * Verifica se uma key já está registrada.
     *
     * <p>UUIDs tornam colisões extremamente improváveis, mas o banco
     * também possui uma restrição única como proteção definitiva.</p>
     *
     * @param objectKey key que será verificada
     * @return true quando a key já estiver registrada
     */
    boolean existsByObjectKey(String objectKey);
}