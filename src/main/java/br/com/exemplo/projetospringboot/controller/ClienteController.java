package br.com.exemplo.projetospringboot.controller;


import br.com.exemplo.projetospringboot.dto.ClienteDTO;
import br.com.exemplo.projetospringboot.service.ClienteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;


@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ClienteDTO>> listar() {

        return ResponseEntity.ok(
                service.listar()
        );
    }

    @GetMapping("/porId/{id}")
    public ResponseEntity<ClienteDTO> buscar(@PathVariable Long id){

        return ResponseEntity.ok(
                service.buscar(id)
        );
    }



    @GetMapping("/ativos")
    public ResponseEntity<List<ClienteDTO>> listarAtivos(){
        return ResponseEntity.ok(
                service.listarAtivos()
        );
    }

    @PostMapping
    public ResponseEntity<ClienteDTO> criar(
            @Valid @RequestBody ClienteDTO dto){

        ClienteDTO cliente = service.criar(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cliente);

    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteDTO> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ClienteDTO dto){

        return ResponseEntity.ok(
                service.atualizar(id,dto)
        );
    }

    @PatchMapping("/{id}/email")
    public ResponseEntity<ClienteDTO> alterarEmail(
            @PathVariable Long id,
            @RequestBody String email){

        return ResponseEntity.ok(
                service.alterarEmail(id,email)
        );
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id){
        service.excluir(id);
        return ResponseEntity
                .noContent()
                .build();

    }





}
