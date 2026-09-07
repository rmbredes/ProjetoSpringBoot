package br.com.exemplo.projetospringboot.service;

import br.com.exemplo.projetospringboot.dto.GerarRelatorioPedidoMensagem;
import br.com.exemplo.projetospringboot.dto.RelatorioPedidoDTO;
import br.com.exemplo.projetospringboot.dto.UrlDownloadRelatorioDTO;
import br.com.exemplo.projetospringboot.entity.Pedido;
import br.com.exemplo.projetospringboot.entity.RelatorioPedido;
import br.com.exemplo.projetospringboot.exception.PublicacaoRelatorioException;
import br.com.exemplo.projetospringboot.exception.RelatorioIndisponivelException;
import br.com.exemplo.projetospringboot.messaging.producer.RelatorioPedidoSqsProducer;
import br.com.exemplo.projetospringboot.repository.PedidoRepository;
import br.com.exemplo.projetospringboot.repository.RelatorioPedidoRepository;
import br.com.exemplo.projetospringboot.storage.S3StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * Coordena a solicitação assíncrona de um relatório de pedido.
 *
 * <p>Esta classe contém o fluxo de negócio. Ela sabe que é necessário
 * registrar a solicitação no banco e depois publicá-la, mas delega os
 * detalhes do Amazon SQS ao {@link RelatorioPedidoSqsProducer}.</p>
 *
 * <p>Não utilizamos uma única transação em volta de todo o método.
 * Uma transação do banco não consegue desfazer uma mensagem que já foi
 * aceita pelo SQS. Por isso, cada estado é salvo explicitamente.</p>
 */
@Service
@ConditionalOnProperty(
        prefix = "application.aws.sqs",
        name = "enabled",
        havingValue = "true"
)
public class RelatorioPedidoService {

    /** Tempo durante o qual a URL assinada será aceita pelo S3. */
    private static final Duration DURACAO_URL_DOWNLOAD =
            Duration.ofMinutes(5);

    /** Logger utilizado para acompanhar o fluxo da solicitação. */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(RelatorioPedidoService.class);

    /** Repositório usado para confirmar a existência do pedido. */
    private final PedidoRepository pedidoRepository;

    /** Repositório que mantém o histórico das solicitações. */
    private final RelatorioPedidoRepository relatorioPedidoRepository;

    /** Componente isolado que envia a mensagem para o SQS. */
    private final RelatorioPedidoSqsProducer producer;

    /** Serviço de armazenamento usado somente para assinar o download. */
    private final S3StorageService storageService;

    /**
     * Recebe as dependências necessárias por injeção do Spring.
     *
     * @param pedidoRepository repositório de pedidos
     * @param relatorioPedidoRepository repositório de relatórios
     * @param producer producer específico do Amazon SQS
     * @param storageService serviço que cria URLs temporárias do S3
     */
    public RelatorioPedidoService(
            PedidoRepository pedidoRepository,
            RelatorioPedidoRepository relatorioPedidoRepository,
            RelatorioPedidoSqsProducer producer,
            S3StorageService storageService
    ) {
        this.pedidoRepository = pedidoRepository;
        this.relatorioPedidoRepository = relatorioPedidoRepository;
        this.producer = producer;
        this.storageService = storageService;
    }

    /**
     * Lista o histórico de relatórios de determinado pedido.
     *
     * <p>A resposta mostrará tanto relatórios concluídos quanto aqueles
     * que ainda estejam pendentes, enfileirados ou com falha.</p>
     *
     * @param pedidoId identificador do pedido
     * @return relatórios mais recentes primeiro
     */
    public List<RelatorioPedidoDTO> listar(Long pedidoId) {
        exigirPedidoExistente(pedidoId);

        return relatorioPedidoRepository
                .findAllByPedidoIdOrderByCriadoEmDesc(pedidoId)
                .stream()
                .map(this::converterParaDTO)
                .toList();
    }

    /**
     * Cria uma URL assinada para baixar um relatório disponível.
     *
     * @param pedidoId identificador do pedido proprietário
     * @param relatorioId identificador interno do relatório
     * @return URL temporária e seu instante de expiração
     */
    public UrlDownloadRelatorioDTO gerarUrlDownload(
            Long pedidoId,
            Long relatorioId
    ) {
        RelatorioPedido relatorio = relatorioPedidoRepository
                .findByIdAndPedidoId(relatorioId, pedidoId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Relatório não encontrado"
                        )
                );

        if (relatorio.getStatus()
                != br.com.exemplo.projetospringboot.enums.StatusRelatorioPedido.DISPONIVEL
                || relatorio.getObjectKey() == null) {
            throw new RelatorioIndisponivelException(
                    "O relatório ainda não está disponível para download"
            );
        }

        Instant expiraEm = Instant.now()
                .plus(DURACAO_URL_DOWNLOAD);

        String url = storageService
                .generateDownloadUrl(
                        relatorio.getObjectKey(),
                        relatorio.getNomeArquivo(),
                        DURACAO_URL_DOWNLOAD
                )
                .toString();

        LOGGER.info(
                "URL temporária de relatório gerada: pedidoId={}, relatorioId={}, expiraEm={}",
                pedidoId,
                relatorioId,
                expiraEm
        );

        return new UrlDownloadRelatorioDTO(
                url,
                expiraEm
        );
    }

    /** Confirma que o pedido existe antes de realizar uma listagem. */
    private void exigirPedidoExistente(Long pedidoId) {
        if (!pedidoRepository.existsById(pedidoId)) {
            throw new NoSuchElementException(
                    "Pedido não encontrado"
            );
        }
    }

    /**
     * Registra e envia uma nova solicitação de relatório.
     *
     * @param pedidoId identificador do pedido solicitado
     * @return estado atualizado da solicitação
     */
    public RelatorioPedidoDTO solicitar(Long pedidoId) {
        Pedido pedido = pedidoRepository
                .findById(pedidoId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Pedido não encontrado"
                        )
                );

        /*
         * Um pedido possui somente um relatório atual neste projeto.
         *
         * Se já houver um registro, devolvemos o mais recente exatamente
         * como está. Portanto, não criamos outra linha no banco, não
         * publicamos outra mensagem e não geramos outro objeto no S3.
         *
         * Os registros duplicados criados antes desta regra permanecem
         * intactos como histórico dos nossos testes.
         */
        Optional<RelatorioPedido> relatorioExistente =
                relatorioPedidoRepository
                .findFirstByPedidoIdOrderByCriadoEmDesc(pedidoId);

        if (relatorioExistente.isPresent()) {
            RelatorioPedido relatorio =
                    relatorioExistente.get();

            LOGGER.info(
                    "Relatório existente reutilizado: relatorioId={}, pedidoId={}, status={}",
                    relatorio.getId(),
                    pedidoId,
                    relatorio.getStatus()
            );

            return converterParaDTO(relatorio);
        }

        UUID solicitacaoId = UUID.randomUUID();

        RelatorioPedido relatorio = new RelatorioPedido(
                solicitacaoId,
                pedido
        );

        /*
         * O primeiro save cria o registro PENDENTE e gera seu ID.
         * Esse ID também será enviado na mensagem.
         */
        relatorio = relatorioPedidoRepository.save(relatorio);

        GerarRelatorioPedidoMensagem mensagem =
                new GerarRelatorioPedidoMensagem(
                        solicitacaoId,
                        relatorio.getId(),
                        pedidoId,
                        relatorio.getCriadoEm()
                );

        try {

            /*
             * Somente depois da confirmação do SQS registramos que
             * a solicitação está efetivamente ENFILEIRADA.
             */
            relatorio.marcarComoEnfileirado();
            relatorio = relatorioPedidoRepository.save(relatorio);

            LOGGER.info(
                    "Relatório solicitado: relatorioId={}, pedidoId={}, solicitacaoId={}, messageId={}",
                    relatorio.getId(),
                    pedidoId,
                    solicitacaoId,
                    producer.enviar(mensagem)
            );

            return converterParaDTO(relatorio);
        } catch (RuntimeException exception) {
            registrarFalha(relatorio, exception);

            throw new PublicacaoRelatorioException(
                    "Não foi possível enviar a solicitação de relatório",
                    exception
            );
        }
    }

    /**
     * Preserva no banco uma descrição curta da falha de publicação.
     */
    private void registrarFalha(
            RelatorioPedido relatorio,
            RuntimeException causa
    ) {
        try {
            relatorio.marcarComoFalha(
                    resumirMensagemErro(causa)
            );

            relatorioPedidoRepository.save(relatorio);
        } catch (RuntimeException falhaAoRegistrar) {
            /*
             * Uma falha ao atualizar o banco não deve esconder
             * a causa original ocorrida durante o envio ao SQS.
             */
            LOGGER.error(
                    "Não foi possível registrar a falha do relatório: solicitacaoId={}",
                    relatorio.getSolicitacaoId(),
                    falhaAoRegistrar
            );
        }
    }

    /**
     * Limita a descrição ao tamanho aceito pela coluna ultimo_erro.
     */
    private String resumirMensagemErro(RuntimeException exception) {
        String mensagem = exception.getMessage();

        if (mensagem == null || mensagem.isBlank()) {
            mensagem = exception
                    .getClass()
                    .getSimpleName();
        }

        int limite = Math.min(mensagem.length(), 2000);

        return mensagem.substring(0, limite);
    }

    /**
     * Converte a entidade para o formato devolvido futuramente pela API.
     */
    private RelatorioPedidoDTO converterParaDTO(
            RelatorioPedido relatorio
    ) {
        return new RelatorioPedidoDTO(
                relatorio.getId(),
                relatorio.getSolicitacaoId(),
                relatorio.getPedido().getId(),
                relatorio.getStatus(),
                relatorio.getNomeArquivo(),
                relatorio.getContentType(),
                relatorio.getTamanhoBytes(),
                relatorio.getChecksumSha256(),
                relatorio.getQuantidadeTentativas(),
                relatorio.getUltimoErro(),
                relatorio.getCriadoEm(),
                relatorio.getAtualizadoEm(),
                relatorio.getConcluidoEm()
        );
    }
}
