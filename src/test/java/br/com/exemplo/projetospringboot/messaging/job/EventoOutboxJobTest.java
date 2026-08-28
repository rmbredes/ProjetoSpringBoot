package br.com.exemplo.projetospringboot.messaging.job;

import br.com.exemplo.projetospringboot.entity.EventoOutbox;
import br.com.exemplo.projetospringboot.enums.StatusEventoOutbox;
import br.com.exemplo.projetospringboot.event.PedidoCriadoEvent;
import br.com.exemplo.projetospringboot.repository.EventoOutboxRepository;
import br.com.exemplo.projetospringboot.service.PublicadorEventoOutboxService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Testes unitários do job responsável por buscar
 * e encaminhar os eventos pendentes da Outbox.
 */
@ExtendWith(MockitoExtension.class)
class EventoOutboxJobTest {

    /**
     * Simula o repositório utilizado para buscar
     * os eventos pendentes.
     */
    @Mock
    private EventoOutboxRepository eventoOutboxRepository;

    /**
     * Simula o serviço responsável por publicar
     * individualmente cada evento.
     */
    @Mock
    private PublicadorEventoOutboxService publicadorEventoOutboxService;

    /**
     * Cria o job testado e injeta automaticamente os mocks.
     */
    @InjectMocks
    private EventoOutboxJob eventoOutboxJob;

    /**
     * Verifica se os eventos pendentes encontrados
     * são enviados individualmente ao publicador.
     */
    @Test
    void devePublicarEventosPendentes() {

        EventoOutbox primeiroEvento = new EventoOutbox(
                UUID.randomUUID(),
                PedidoCriadoEvent.class.getSimpleName(),
                "pedidos-criados",
                "10",
                "{}"
        );

        EventoOutbox segundoEvento = new EventoOutbox(
                UUID.randomUUID(),
                PedidoCriadoEvent.class.getSimpleName(),
                "pedidos-criados",
                "20",
                "{}"
        );

        /*
         * Define manualmente os identificadores para representar
         * os valores que seriam gerados pelo banco.
         *
         * Como a entidade não possui setter para o ID, utilizaremos
         * o mock do próprio EventoOutbox neste teste.
         */
        EventoOutbox primeiroEventoMock = mock(EventoOutbox.class);
        EventoOutbox segundoEventoMock = mock(EventoOutbox.class);

        when(
                primeiroEventoMock.getId()
        ).thenReturn(1L);

        when(
                segundoEventoMock.getId()
        ).thenReturn(2L);

        /*
         * Captura a paginação utilizada pelo job para validar
         * o tamanho configurado para o lote.
         */
        ArgumentCaptor<Pageable> pageableCaptor =
                ArgumentCaptor.forClass(Pageable.class);

        when(
                eventoOutboxRepository.buscarPorStatus(
                        eq(StatusEventoOutbox.PENDENTE),
                        pageableCaptor.capture()
                )
        ).thenReturn(
                List.of(
                        primeiroEventoMock,
                        segundoEventoMock
                )
        );

        /*
         * Executa o método diretamente.
         *
         * Não precisamos esperar o @Scheduled, pois estamos
         * testando o comportamento do método, não o relógio.
         */
        eventoOutboxJob.publicarEventosPendentes();

        /*
         * Confirma que cada registro encontrado foi enviado
         * individualmente ao serviço publicador.
         */
        verify(
                publicadorEventoOutboxService
        ).publicar(1L);

        verify(
                publicadorEventoOutboxService
        ).publicar(2L);

        /*
         * Confirma que o job buscou a primeira página
         * contendo no máximo 20 registros.
         */
        Pageable pageableUtilizado =
                pageableCaptor.getValue();

        assertEquals(
                0,
                pageableUtilizado.getPageNumber()
        );

        assertEquals(
                20,
                pageableUtilizado.getPageSize()
        );
    }
}