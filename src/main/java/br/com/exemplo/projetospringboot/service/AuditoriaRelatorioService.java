package br.com.exemplo.projetospringboot.service;

import br.com.exemplo.projetospringboot.dto.AuditoriaRelatorioDTO;
import br.com.exemplo.projetospringboot.dto.RelatorioPedidoDisponivelEvento;
import br.com.exemplo.projetospringboot.entity.AuditoriaRelatorio;
import br.com.exemplo.projetospringboot.repository.AuditoriaRelatorioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Aplica a regra de negócio da auditoria de relatórios.
 *
 * <p>A classe não conhece SQS ou SNS. Ela recebe um evento Java,
 * evita duplicidade e persiste o registro correspondente.</p>
 */
@Service
public class AuditoriaRelatorioService {

    private static final String TIPO_ESPERADO =
            "RELATORIO_PEDIDO_DISPONIVEL";

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AuditoriaRelatorioService.class);

    private final AuditoriaRelatorioRepository repository;

    public AuditoriaRelatorioService(
            AuditoriaRelatorioRepository repository
    ) {
        this.repository = repository;
    }

    /**
     * Registra o evento caso ele ainda não tenha sido processado.
     *
     * <p>O eventoId funciona como chave de idempotência. Se o SQS
     * entregar a mesma mensagem novamente, não criaremos outra linha.</p>
     *
     * @param evento evento recebido da fila de auditoria
     */
    @Transactional
    public void registrar(
            RelatorioPedidoDisponivelEvento evento
    ) {
        validarEvento(evento);

        if (repository.existsByEventoId(evento.eventoId())) {
            LOGGER.info(
                    "Evento de auditoria duplicado ignorado: eventoId={}",
                    evento.eventoId()
            );
            return;
        }

        AuditoriaRelatorio auditoria = new AuditoriaRelatorio(
                evento.eventoId(),
                evento.tipo(),
                evento.relatorioId(),
                evento.pedidoId(),
                evento.solicitacaoId(),
                evento.objectKey(),
                evento.ocorridoEm(),
                Instant.now()
        );

        repository.save(auditoria);

        LOGGER.info(
                "Auditoria de relatório registrada: eventoId={}, relatorioId={}, pedidoId={}",
                evento.eventoId(),
                evento.relatorioId(),
                evento.pedidoId()
        );
    }

    /** Lista as auditorias de um pedido para consulta pela API. */
    @Transactional(readOnly = true)
    public List<AuditoriaRelatorioDTO> listarPorPedido(
            Long pedidoId
    ) {
        return repository
                .findAllByPedidoIdOrderByRecebidoEmDesc(pedidoId)
                .stream()
                .map(this::converterParaDTO)
                .toList();
    }

    /** Rejeita mensagens de outro tipo antes de gravar dados incorretos. */
    private void validarEvento(
            RelatorioPedidoDisponivelEvento evento
    ) {
        if (evento.eventoId() == null) {
            throw new IllegalArgumentException(
                    "O evento de auditoria não possui eventoId"
            );
        }

        if (!TIPO_ESPERADO.equals(evento.tipo())) {
            throw new IllegalArgumentException(
                    "Tipo de evento não suportado pela auditoria: "
                            + evento.tipo()
            );
        }
    }

    /** Mantém a entidade interna separada do contrato REST. */
    private AuditoriaRelatorioDTO converterParaDTO(
            AuditoriaRelatorio auditoria
    ) {
        return new AuditoriaRelatorioDTO(
                auditoria.getId(),
                auditoria.getEventoId(),
                auditoria.getTipo(),
                auditoria.getRelatorioId(),
                auditoria.getPedidoId(),
                auditoria.getSolicitacaoId(),
                auditoria.getObjectKey(),
                auditoria.getOcorridoEm(),
                auditoria.getRecebidoEm()
        );
    }
}
