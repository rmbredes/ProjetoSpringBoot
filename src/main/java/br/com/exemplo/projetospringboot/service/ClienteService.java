package br.com.exemplo.projetospringboot.service;

import br.com.exemplo.projetospringboot.dto.ClienteDTO;
import br.com.exemplo.projetospringboot.entity.Cliente;
import br.com.exemplo.projetospringboot.repository.ClienteRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Centraliza as regras de negócio e as operações de cache relacionadas aos clientes.
 */
@Service
public class ClienteService {

    /** Repositório utilizado para consultar e persistir clientes. */
    public final ClienteRepository clienteRepository;

    /**
     * Recebe as dependências necessárias para executar as operações de clientes.
     *
     * @param clienteRepository repositório de clientes gerenciado pelo Spring
     */
    public ClienteService(ClienteRepository clienteRepository) {
        // Guarda a dependência recebida para uso pelos métodos do serviço.
        this.clienteRepository = clienteRepository;
    }

    /**
     * Busca um cliente pelo identificador e armazena o resultado no cache.
     *
     * @param id identificador do cliente
     * @return dados do cliente encontrado
     */
    @Cacheable(value = "clientes", key = "#id")
    public ClienteDTO buscar(Long id) {
        // Consulta o banco apenas quando o cliente não está disponível no cache.
        Cliente cliente = clienteRepository.findById(id).orElseThrow();
        // Converte a entidade antes de devolvê-la à camada externa.
        return converterParaDTO(cliente);
    }

    /**
     * Lista todos os clientes cadastrados.
     *
     * @return clientes convertidos para DTO
     */
    public List<ClienteDTO> listar() {
        // Converte cada entidade retornada pelo repositório em um DTO.
        return clienteRepository.findAll()
                .stream()
                .map(this::converterParaDTO)
                .toList();

    }

    /**
     * Lista somente os clientes ativos.
     *
     * @return clientes ativos convertidos para DTO
     */
    public List<ClienteDTO> listarAtivos() {
        // Usa a consulta derivada do Spring Data e converte cada resultado.
        return clienteRepository.findByAtivoTrue()
                .stream()
                .map(this::converterParaDTO)
                .toList();


    }

    /**
     * Cria um cliente a partir dos dados recebidos.
     *
     * @param clienteDTO dados do novo cliente
     * @return cliente persistido
     */
    public ClienteDTO criar(ClienteDTO clienteDTO)
    {
        // Cria a entidade que será persistida no banco.
        Cliente cliente = new Cliente();
        // Copia para a entidade os dados recebidos na requisição.
        cliente.setNome(clienteDTO.nome());
        cliente.setEmail(clienteDTO.email());
        cliente.setAtivo(clienteDTO.ativo());

        // Persiste o novo cliente e recebe a entidade com o identificador gerado.
        Cliente salvo = clienteRepository.save(cliente);
        // Devolve somente a representação pública do cliente salvo.
        return converterParaDTO(salvo);

    }

    /**
     * Atualiza todos os dados editáveis de um cliente e renova seu cache.
     *
     * @param id identificador do cliente
     * @param clienteDTO novos dados do cliente
     * @return cliente atualizado
     */
    @CachePut(value = "clientes", key = "#id")
    public ClienteDTO atualizar(Long id, ClienteDTO clienteDTO)
    {
        // Recupera a entidade existente ou interrompe a operação quando ela não existe.
        Cliente cliente = buscarEntidade(id);
        // Substitui os dados atuais pelos valores recebidos.
        cliente.setNome(clienteDTO.nome());
        cliente.setEmail(clienteDTO.email());
        cliente.setAtivo(clienteDTO.ativo());

        // Salva as alterações no banco e atualiza o valor retornado pelo cache.
        Cliente atualizado = clienteRepository.save(cliente);
        return converterParaDTO(atualizado);
    }

    /**
     * Altera apenas o e-mail do cliente e renova seu cache.
     *
     * @param id identificador do cliente
     * @param email novo endereço de e-mail
     * @return cliente atualizado
     */
    @CachePut(value = "clientes", key = "#id")
    public ClienteDTO alterarEmail(Long id, String email){
        // Recupera o cliente que receberá o novo e-mail.
        Cliente cliente = buscarEntidade(id);
        // Modifica somente o campo solicitado.
        cliente.setEmail(email);
        // Persiste a alteração antes de converter o resultado.
        Cliente atualizado = clienteRepository.save(cliente);
        return converterParaDTO(atualizado);
    }

    /**
     * Exclui um cliente e remove sua entrada do cache.
     *
     * @param id identificador do cliente
     */
    @CacheEvict(value = "clientes", key = "#id")
    public void excluir(Long id){
        // Confirma a existência do cliente antes de solicitar a exclusão.
        Cliente cliente = buscarEntidade(id);
        // Remove a entidade persistida.
        clienteRepository.delete(cliente);
    }

    /**
     * Converte a entidade persistida para o formato exposto pela aplicação.
     *
     * @param cliente entidade de cliente
     * @return representação DTO do cliente
     */
    private ClienteDTO converterParaDTO(Cliente cliente) {
        // Monta um DTO imutável usando os valores da entidade.
        return new ClienteDTO(
                cliente.getId(),
                cliente.getNome(),
                cliente.getEmail(),
                cliente.isAtivo()
        );


    }

    /**
     * Recupera internamente uma entidade de cliente pelo identificador.
     *
     * @param id identificador procurado
     * @return entidade encontrada
     */
    private Cliente buscarEntidade(Long id){
        // Mantém o Optional até a validação final da existência do cliente.
        Optional<Cliente>  cliente = clienteRepository.findById(id);
        // Devolve a entidade ou lança a exceção padrão quando não houver resultado.
        return cliente.orElseThrow();

    }

}

