package br.com.exemplo.projetospringboot.controller;


import br.com.exemplo.projetospringboot.dto.ClienteDTO;
import br.com.exemplo.projetospringboot.service.ClienteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Disponibiliza as operações HTTP relacionadas aos clientes.
 *
 * O controller recebe e devolve DTOs, enquanto as regras
 * de negócio permanecem no ClienteService.
 */
@RestController
@RequestMapping("/clientes")
public class ClienteController {

    /** Serviço responsável pelas regras e persistência dos clientes. */
    private final ClienteService service;

    /**
     * Recebe o serviço por injeção de dependência.
     *
     * @param service serviço de clientes
     */
    public ClienteController(ClienteService service) {
        this.service = service;
    }

    /**
     * Lista todos os clientes cadastrados.
     *
     * @return clientes encontrados com status HTTP 200
     */
    @GetMapping
    public ResponseEntity<List<ClienteDTO>> listar() {
        /* Encapsula a lista devolvida pelo serviço em uma resposta HTTP. */
        return ResponseEntity.ok(
                service.listar()
        );
    }

    /**
     * Busca um cliente pelo seu identificador.
     *
     * @param id identificador recebido na URL
     * @return cliente encontrado com status HTTP 200
     */
    @GetMapping("/porId/{id}")
    public ResponseEntity<ClienteDTO> buscar(@PathVariable Long id){
        /* Delega a busca e deixa o handler global tratar ausências. */
        return ResponseEntity.ok(
                service.buscar(id)
        );
    }

    /**
     * Lista somente os clientes que permanecem ativos.
     *
     * @return clientes ativos com status HTTP 200
     */
    @GetMapping("/ativos")
    public ResponseEntity<List<ClienteDTO>> listarAtivos(){
        /* Solicita ao serviço a visão filtrada dos clientes. */
        return ResponseEntity.ok(
                service.listarAtivos()
        );
    }

    /**
     * Cadastra um novo cliente após validar o corpo da requisição.
     *
     * @param dto dados enviados pelo cliente HTTP
     * @return cliente criado com status HTTP 201
     */
    @PostMapping
    public ResponseEntity<ClienteDTO> criar(
            @Valid @RequestBody ClienteDTO dto){
        /* Executa o cadastro e guarda o DTO já preenchido pelo serviço. */
        ClienteDTO cliente = service.criar(dto);

        /* Informa explicitamente que um novo recurso foi criado. */
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cliente);
    }

    /**
     * Substitui os dados editáveis de um cliente existente.
     *
     * @param id identificador do cliente
     * @param dto novos dados validados
     * @return cliente atualizado com status HTTP 200
     */
    @PutMapping("/{id}")
    public ResponseEntity<ClienteDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ClienteDTO dto){
        /* Delega a atualização integral ao serviço. */
        return ResponseEntity.ok(
                service.atualizar(id,dto)
        );
    }

    /**
     * Altera somente o e-mail do cliente indicado.
     *
     * @param id identificador do cliente
     * @param email novo e-mail recebido no corpo
     * @return cliente atualizado com status HTTP 200
     */
    @PatchMapping("/{id}/email")
    public ResponseEntity<ClienteDTO> alterarEmail(
            @PathVariable Long id,
            @RequestBody String email){
        /* Executa uma alteração parcial sem substituir os demais campos. */
        return ResponseEntity.ok(
                service.alterarEmail(id,email)
        );
    }

    /**
     * Exclui o cliente indicado.
     *
     * @param id identificador do cliente
     * @return resposta sem corpo com status HTTP 204
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id){
        /* Solicita a exclusão antes de construir a resposta vazia. */
        service.excluir(id);

        /* O status 204 informa que a operação terminou sem conteúdo. */
        return ResponseEntity
                .noContent()
                .build();
    }
}
