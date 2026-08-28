package br.com.exemplo.projetospringboot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuração responsável por habilitar a execução
 * de tarefas agendadas na aplicação.
 *
 * Depois desta configuração, o Spring reconhecerá métodos
 * anotados com @Scheduled.
 */
@Configuration
@EnableScheduling
public class AgendamentoConfig {

    /*
     * Esta classe não precisa possuir métodos.
     *
     * A anotação @EnableScheduling é suficiente para habilitar
     * o mecanismo de agendamento do Spring.
     */
}