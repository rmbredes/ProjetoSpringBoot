package br.com.exemplo.projetospringboot.repository;

import br.com.exemplo.projetospringboot.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Fornece as operações de persistência e as consultas específicas de clientes.
 */
public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    /**
     * Busca somente os clientes que estão marcados como ativos.
     *
     * @return lista de clientes ativos
     */
    List<Cliente> findByAtivoTrue();

    /**
     * Busca clientes cujo nome contenha o texto informado, ignorando maiúsculas e minúsculas.
     *
     * @param nome trecho usado na pesquisa
     * @return clientes encontrados
     */
    List<Cliente> findByNomeContainingIgnoreCase(String nome);
}
