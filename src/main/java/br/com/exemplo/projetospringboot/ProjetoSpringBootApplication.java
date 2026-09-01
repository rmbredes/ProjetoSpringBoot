package br.com.exemplo.projetospringboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Ponto de entrada da aplicação Spring Boot.
 *
 * A anotação habilita a configuração automática e a busca
 * dos componentes existentes neste pacote e em seus subpacotes.
 */
@SpringBootApplication
public class ProjetoSpringBootApplication {

    /**
     * Inicializa o contexto do Spring e o servidor web embutido.
     *
     * @param args argumentos recebidos pela linha de comando
     */
    public static void main(String[] args) {
        /*
         * Entrega ao Spring a classe principal e os argumentos
         * utilizados para configurar a execução.
         */
        SpringApplication.run(ProjetoSpringBootApplication.class, args);
    }
}
