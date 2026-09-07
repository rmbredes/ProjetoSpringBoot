package br.com.exemplo.projetospringboot.service;

import br.com.exemplo.projetospringboot.dto.AnexoPedidoDTO;
import br.com.exemplo.projetospringboot.dto.UrlDownloadAnexoDTO;
import br.com.exemplo.projetospringboot.entity.AnexoPedido;
import br.com.exemplo.projetospringboot.entity.Pedido;
import br.com.exemplo.projetospringboot.enums.StatusAnexoPedido;
import br.com.exemplo.projetospringboot.exception.ArmazenamentoAnexoException;
import br.com.exemplo.projetospringboot.exception.ArquivoAnexoInvalidoException;
import br.com.exemplo.projetospringboot.repository.AnexoPedidoRepository;
import br.com.exemplo.projetospringboot.repository.PedidoRepository;
import br.com.exemplo.projetospringboot.storage.S3StorageService;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

/**
 * Aplica as regras de negócio relacionadas aos anexos dos pedidos.
 *
 * <p>Este serviço coordena três responsabilidades:</p>
 *
 * <ol>
 *     <li>validar o arquivo recebido;</li>
 *     <li>salvar os metadados no banco;</li>
 *     <li>enviar o conteúdo ao Amazon S3.</li>
 * </ol>
 *
 * <p>Os detalhes técnicos do S3 permanecem encapsulados no
 * S3StorageService.</p>
 */
@Service
public class AnexoPedidoService {

    /**
     * Limite de 10 MB expresso em bytes.
     *
     * <p>Um megabyte utilizado aqui corresponde a 1024 × 1024 bytes.</p>
     */
    private static final long TAMANHO_MAXIMO_BYTES =
            10L * 1024L * 1024L;

    /**
     * Tipos MIME aceitos nesta primeira versão.
     *
     * <p>O Set permite verificar rapidamente se determinado
     * tipo está presente.</p>
     */
    private static final Set<String> CONTENT_TYPES_PERMITIDOS =
            Set.of(
                    "application/pdf",
                    "image/jpeg",
                    "image/png",
                    "text/plain"
            );

    /**
     * Limite compatível com a coluna nome_original.
     */
    private static final int TAMANHO_MAXIMO_NOME = 255;

    /**
     * Tempo de validade concedido a cada URL pré-assinada.
     */
    private static final Duration DURACAO_URL_DOWNLOAD =
            Duration.ofMinutes(5);

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AnexoPedidoService.class);

    private final PedidoRepository pedidoRepository;
    private final AnexoPedidoRepository anexoPedidoRepository;
    private final S3StorageService storageService;
    private final ObservationRegistry observationRegistry;

    /**
     * Recebe todas as dependências necessárias para coordenar o upload.
     */
    public AnexoPedidoService(
            PedidoRepository pedidoRepository,
            AnexoPedidoRepository anexoPedidoRepository,
            S3StorageService storageService,
            ObservationRegistry observationRegistry
    ) {
        this.pedidoRepository = pedidoRepository;
        this.anexoPedidoRepository = anexoPedidoRepository;
        this.storageService = storageService;
        this.observationRegistry = observationRegistry;
    }

    /**
     * Valida, registra e envia um anexo.
     *
     * <p>A observação produzirá informações de tracing e duração
     * aproveitando a infraestrutura de observabilidade que já
     * configuramos anteriormente.</p>
     *
     * @param pedidoId identificador do pedido
     * @param arquivo conteúdo recebido por multipart
     * @return representação do anexo disponível
     */
    public AnexoPedidoDTO enviar(
            Long pedidoId,
            MultipartFile arquivo
    ) {
        return Observation
                .createNotStarted(
                        "anexo.upload",
                        observationRegistry
                )
                /*
                 * Esta tag possui poucos valores possíveis e pode ser
                 * utilizada com segurança em métricas.
                 */
                .lowCardinalityKeyValue(
                        "storage",
                        "s3"
                )
                .observe(() ->
                        executarEnvio(pedidoId, arquivo)
                );
    }

    /**
     * Lista somente os anexos disponíveis de determinado pedido.
     *
     * <p>Registros PENDENTE, FALHA ou EXCLUIDO permanecem no banco
     * para diagnóstico e histórico, mas não são apresentados como
     * arquivos disponíveis ao usuário.</p>
     *
     * @param pedidoId identificador do pedido
     * @return anexos disponíveis, do mais recente para o mais antigo
     */
    public List<AnexoPedidoDTO> listar(Long pedidoId) {
        return Observation
                .createNotStarted(
                        "anexo.listar",
                        observationRegistry
                )
                .observe(() -> {
                    exigirPedidoExistente(pedidoId);

                    return anexoPedidoRepository
                            .findAllByPedidoIdAndStatusOrderByCriadoEmDesc(
                                    pedidoId,
                                    StatusAnexoPedido.DISPONIVEL
                            )
                            .stream()
                            .map(this::converterParaDTO)
                            .toList();
                });
    }

    /**
     * Gera uma autorização temporária de download para um anexo.
     *
     * @param pedidoId identificador do pedido proprietário
     * @param anexoId identificador interno do anexo
     * @return URL assinada e seu instante de expiração
     */
    public UrlDownloadAnexoDTO gerarUrlDownload(
            Long pedidoId,
            Long anexoId
    ) {
        return Observation
                .createNotStarted(
                        "anexo.download.url",
                        observationRegistry
                )
                .lowCardinalityKeyValue(
                        "storage",
                        "s3"
                )
                .observe(() -> {
                    AnexoPedido anexo =
                            buscarAnexoDisponivel(
                                    pedidoId,
                                    anexoId
                            );

                    Instant expiraEm = Instant.now()
                            .plus(DURACAO_URL_DOWNLOAD);

                    String url = storageService
                            .generateDownloadUrl(
                                    anexo.getObjectKey(),
                                    anexo.getNomeOriginal(),
                                    DURACAO_URL_DOWNLOAD
                            )
                            .toString();

                    LOGGER.info(
                            "URL temporária gerada: pedidoId={}, anexoId={}, expiraEm={}",
                            pedidoId,
                            anexoId,
                            expiraEm
                    );

                    return new UrlDownloadAnexoDTO(
                            url,
                            expiraEm
                    );
                });
    }

    /**
     * Remove o objeto do S3 e registra sua exclusão lógica no banco.
     *
     * <p>O registro não é apagado fisicamente para preservar o
     * histórico. Ele deixa de aparecer na listagem porque passa
     * para o estado EXCLUIDO.</p>
     *
     * @param pedidoId identificador do pedido proprietário
     * @param anexoId identificador do anexo
     */
    public void excluir(
            Long pedidoId,
            Long anexoId
    ) {
        Observation
                .createNotStarted(
                        "anexo.excluir",
                        observationRegistry
                )
                .lowCardinalityKeyValue(
                        "storage",
                        "s3"
                )
                .observe(() -> {
                    AnexoPedido anexo =
                            buscarAnexoDisponivel(
                                    pedidoId,
                                    anexoId
                            );

                    try {
                        storageService.delete(
                                anexo.getObjectKey()
                        );
                    } catch (RuntimeException exception) {
                        throw new ArmazenamentoAnexoException(
                                "Não foi possível excluir o anexo",
                                exception
                        );
                    }

                    anexo.marcarComoExcluido();
                    anexoPedidoRepository.save(anexo);

                    LOGGER.info(
                            "Anexo excluído: pedidoId={}, anexoId={}, objectKey={}",
                            pedidoId,
                            anexoId,
                            anexo.getObjectKey()
                    );
                });
    }

    /**
     * Executa o fluxo interno observado pelo método público.
     *
     * <p>Não colocamos uma única transação em volta de todo o método.
     * Uma transação do banco não consegue desfazer automaticamente
     * um upload realizado no S3.</p>
     */
    private AnexoPedidoDTO executarEnvio(
            Long pedidoId,
            MultipartFile arquivo
    ) {
        validarArquivo(arquivo);

        Pedido pedido = pedidoRepository
                .findById(pedidoId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Pedido não encontrado"
                        )
                );

        /*
         * O limite de 10 MB permite manter os bytes em memória nesta
         * primeira versão. Assim calculamos o SHA-256 sobre exatamente
         * o mesmo conteúdo que será enviado.
         *
         * Para arquivos muito maiores, utilizaríamos streaming.
         */
        byte[] conteudo = lerConteudo(arquivo);

        String nomeOriginal =
                normalizarNome(arquivo.getOriginalFilename());

        String contentType = arquivo.getContentType();

        String checksumSha256 =
                calcularSha256(conteudo);

        String objectKey = criarObjectKey(
                pedidoId
        );

        /*
         * Primeiro registramos o estado PENDENTE.
         *
         * Se a aplicação parar durante o upload, o banco preservará
         * evidência de uma operação incompleta.
         */
        AnexoPedido anexo = new AnexoPedido(
                pedido,
                objectKey,
                nomeOriginal,
                contentType,
                conteudo.length,
                checksumSha256
        );

        anexo = anexoPedidoRepository.save(anexo);

        try {
            String eTag = storageService.upload(
                    objectKey,
                    new ByteArrayInputStream(conteudo),
                    conteudo.length,
                    contentType
            );

            anexo.marcarComoDisponivel(eTag);

            AnexoPedido anexoDisponivel =
                    anexoPedidoRepository.save(anexo);

            LOGGER.info(
                    "Anexo armazenado: pedidoId={}, anexoId={}, objectKey={}, tamanhoBytes={}",
                    pedidoId,
                    anexoDisponivel.getId(),
                    objectKey,
                    conteudo.length
            );

            return converterParaDTO(anexoDisponivel);
        } catch (RuntimeException exception) {
            /*
             * Tentamos registrar a falha no banco.
             *
             * Esse registro facilita diagnóstico e uma futura rotina
             * de reprocessamento.
             */
            registrarFalha(anexo, exception);

            throw new ArmazenamentoAnexoException(
                    "Não foi possível armazenar o anexo",
                    exception
            );
        }
    }

    /**
     * Aplica as validações que não dependem do conteúdo completo.
     */
    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ArquivoAnexoInvalidoException(
                    "O arquivo não pode estar vazio"
            );
        }

        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            throw new ArquivoAnexoInvalidoException(
                    "O arquivo deve possuir no máximo 10 MB"
            );
        }

        String nomeOriginal =
                normalizarNome(arquivo.getOriginalFilename());

        if (nomeOriginal.length() > TAMANHO_MAXIMO_NOME) {
            throw new ArquivoAnexoInvalidoException(
                    "O nome do arquivo deve possuir no máximo 255 caracteres"
            );
        }

        String contentType = arquivo.getContentType();

        if (contentType == null
                || !CONTENT_TYPES_PERMITIDOS.contains(contentType)) {

            throw new ArquivoAnexoInvalidoException(
                    "Tipo de arquivo não permitido. "
                            + "Utilize PDF, JPG, PNG ou TXT"
            );
        }
    }

    /**
     * Remove qualquer caminho eventualmente enviado junto com o nome.
     *
     * <p>Exemplo:</p>
     *
     * <pre>
     * C:\documentos\contrato.pdf → contrato.pdf
     * </pre>
     */
    private String normalizarNome(String nomeRecebido) {
        if (nomeRecebido == null || nomeRecebido.isBlank()) {
            throw new ArquivoAnexoInvalidoException(
                    "O arquivo precisa possuir um nome"
            );
        }

        /*
         * Converte a barra do Windows para um único formato,
         * permitindo localizar o último segmento do caminho.
         */
        String nomeNormalizado =
                nomeRecebido.replace('\\', '/');

        int ultimaBarra =
                nomeNormalizado.lastIndexOf('/');

        if (ultimaBarra >= 0) {
            nomeNormalizado =
                    nomeNormalizado.substring(ultimaBarra + 1);
        }

        if (nomeNormalizado.isBlank()) {
            throw new ArquivoAnexoInvalidoException(
                    "O arquivo precisa possuir um nome válido"
            );
        }

        return nomeNormalizado;
    }

    /**
     * Lê os bytes recebidos pelo Spring.
     */
    private byte[] lerConteudo(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException exception) {
            throw new ArmazenamentoAnexoException(
                    "Não foi possível ler o arquivo recebido",
                    exception
            );
        }
    }

    /**
     * Calcula o hash SHA-256 e o converte para hexadecimal.
     */
    private String calcularSha256(byte[] conteudo) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(conteudo);

            return HexFormat
                    .of()
                    .formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            /*
             * Toda implementação moderna do Java precisa fornecer
             * SHA-256. Se ele não existir, o ambiente Java está inválido.
             */
            throw new IllegalStateException(
                    "SHA-256 não está disponível",
                    exception
            );
        }
    }

    /**
     * Cria uma key sem utilizar o nome fornecido pelo usuário.
     */
    private String criarObjectKey(Long pedidoId) {
        return "pedidos/"
                + pedidoId
                + "/anexos/"
                + UUID.randomUUID();
    }

    /**
     * Confirma que o pedido informado existe antes da listagem.
     */
    private void exigirPedidoExistente(Long pedidoId) {
        if (!pedidoRepository.existsById(pedidoId)) {
            throw new NoSuchElementException(
                    "Pedido não encontrado"
            );
        }
    }

    /**
     * Localiza um anexo dentro de seu pedido e exige que ele esteja
     * disponível para download ou exclusão.
     */
    private AnexoPedido buscarAnexoDisponivel(
            Long pedidoId,
            Long anexoId
    ) {
        AnexoPedido anexo = anexoPedidoRepository
                .findByIdAndPedidoId(anexoId, pedidoId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Anexo não encontrado"
                        )
                );

        if (anexo.getStatus()
                != StatusAnexoPedido.DISPONIVEL) {
            throw new NoSuchElementException(
                    "Anexo não encontrado"
            );
        }

        return anexo;
    }

    /**
     * Tenta preservar no banco a causa da falha de upload.
     */
    private void registrarFalha(
            AnexoPedido anexo,
            RuntimeException causa
    ) {
        try {
            String mensagem =
                    resumirMensagemErro(causa);

            anexo.marcarComoFalha(mensagem);
            anexoPedidoRepository.save(anexo);
        } catch (RuntimeException falhaAoRegistrar) {
            /*
             * A falha de persistência não substitui a causa original.
             * Ambas ficam registradas no log.
             */
            LOGGER.error(
                    "Não foi possível registrar a falha do anexo: objectKey={}",
                    anexo.getObjectKey(),
                    falhaAoRegistrar
            );
        }
    }

    /**
     * Limita a mensagem ao tamanho aceito pela coluna ultimo_erro.
     */
    private String resumirMensagemErro(RuntimeException exception) {
        String mensagem = exception.getMessage();

        if (mensagem == null || mensagem.isBlank()) {
            mensagem = exception
                    .getClass()
                    .getSimpleName();
        }

        int limite = Math.min(
                mensagem.length(),
                2000
        );

        return mensagem.substring(0, limite);
    }

    /**
     * Converte a entidade para a representação pública da API.
     */
    private AnexoPedidoDTO converterParaDTO(AnexoPedido anexo) {
        return new AnexoPedidoDTO(
                anexo.getId(),
                anexo.getPedido().getId(),
                anexo.getNomeOriginal(),
                anexo.getContentType(),
                anexo.getTamanhoBytes(),
                anexo.getChecksumSha256(),
                anexo.getStatus(),
                anexo.getCriadoEm()
        );
    }
}
