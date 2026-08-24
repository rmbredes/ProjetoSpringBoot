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

@Service
public class ClienteService {


    public final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Cacheable(value = "clientes", key = "#id")
    public ClienteDTO buscar(Long id) {
        Cliente cliente = clienteRepository.findById(id).orElseThrow();
        return converterParaDTO(cliente);
    }

    public List<ClienteDTO> listar() {
        return clienteRepository.findAll()
                .stream()
                .map(this::converterParaDTO)
                .toList();

    }

    public List<ClienteDTO> listarAtivos() {
        return clienteRepository.findByAtivoTrue()
                .stream()
                .map(this::converterParaDTO)
                .toList();


    }

    public ClienteDTO criar(ClienteDTO clienteDTO)
    {
        Cliente cliente = new Cliente();
        cliente.setNome(clienteDTO.nome());
        cliente.setEmail(clienteDTO.email());
        cliente.setAtivo(clienteDTO.ativo());

        Cliente salvo = clienteRepository.save(cliente);
        return converterParaDTO(salvo);

    }
    @CachePut(value = "clientes", key = "#id")
    public ClienteDTO atualizar(Long id, ClienteDTO clienteDTO)
    {

        Cliente cliente = buscarEntidade(id);
        cliente.setNome(clienteDTO.nome());
        cliente.setEmail(clienteDTO.email());
        cliente.setAtivo(clienteDTO.ativo());

        Cliente atualizado = clienteRepository.save(cliente);
        return converterParaDTO(atualizado);
    }
    @CachePut(value = "clientes", key = "#id")
    public ClienteDTO alterarEmail(Long id, String email){
        Cliente cliente = buscarEntidade(id);
        cliente.setEmail(email);
        Cliente atualizado = clienteRepository.save(cliente);
        return converterParaDTO(atualizado);
    }
    @CacheEvict(value = "clientes", key = "#id")
    public void excluir(Long id){
        Cliente cliente = buscarEntidade(id);
        clienteRepository.delete(cliente);
    }


    private ClienteDTO converterParaDTO(Cliente cliente) {

        return new ClienteDTO(
                cliente.getId(),
                cliente.getNome(),
                cliente.getEmail(),
                cliente.isAtivo()
        );


    }

    private Cliente buscarEntidade(Long id){
        Optional<Cliente>  cliente = clienteRepository.findById(id);
        return cliente.orElseThrow();

    }

}

