package kz.edu.soccerhub.crm.application.service;
import kz.edu.soccerhub.common.dto.lead.LeadWorkCommand;
import kz.edu.soccerhub.common.exception.*;
import kz.edu.soccerhub.crm.domain.model.Lead;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.crm.domain.repository.LeadRepository;
import kz.edu.soccerhub.crm.application.state.LeadEvent;
import org.junit.jupiter.api.*;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class LeadWorkServiceTest {
    LeadRepository repo = mock(LeadRepository.class);
    LeadActivityService history = mock(LeadActivityService.class);
    LeadService leads = mock(LeadService.class);
    LeadWorkService service = new LeadWorkService(repo, history, leads, new com.fasterxml.jackson.databind.ObjectMapper());
    UUID id = UUID.randomUUID(), actor = UUID.randomUUID();
    Lead lead;
    @BeforeEach void setUp() {
        lead = Lead.builder().id(id).status(LeadStatus.NEW).build();
        when(repo.findLockedById(id)).thenReturn(Optional.of(lead));
    }
    LeadWorkCommand.LeadWorkCommandBuilder base() {
        return LeadWorkCommand.builder().version(0L).priority(LeadWorkCommand.Priority.HIGH).operation(LeadWorkCommand.Operation.PLAN);
    }
    @Test void savesPlanAndJournal() {
        Instant due = Instant.now().plusSeconds(3600);
        service.update(id, actor, base().nextAction(" Позвонить ").nextActionAt(due).build());
        assertEquals("Позвонить", lead.getNextAction()); assertEquals(due, lead.getNextActionAt());
        assertEquals(1, lead.getWorkVersion()); verify(history).logWork(eq(lead), eq(actor), contains("Позвонить"));
    }
    @Test void rejectsStaleVersion() {
        assertThrows(ConflictException.class, () -> service.update(id, actor, base().version(5L).build()));
        verifyNoInteractions(history);
    }
    @Test void rejectsActionWithoutDueDate() {
        assertThrows(BadRequestException.class, () -> service.update(id, actor, base().nextAction("Позвонить").build()));
    }
    @Test void rejectsDateWithoutAction() {
        assertThrows(BadRequestException.class, () -> service.update(id, actor, base().nextActionAt(Instant.now().plusSeconds(100)).build()));
    }
    @Test void rejectsNewPastDate() {
        assertThrows(BadRequestException.class, () -> service.update(id, actor, base().nextAction("Позвонить").nextActionAt(Instant.now().minusSeconds(100)).build()));
    }
    @Test void keepsExistingOverdueTaskWhenLoggingContact() {
        Instant due = Instant.now().minusSeconds(100);
        lead.updateWork("NORMAL", "Позвонить", due, false);
        service.update(id, actor, base().version(1L).operation(LeadWorkCommand.Operation.CONTACT)
                .channel(LeadWorkCommand.Channel.PHONE).outcome(LeadWorkCommand.Outcome.NO_ANSWER)
                .nextAction("Позвонить").nextActionAt(due).build());
        assertEquals(due, lead.getNextActionAt()); assertNotNull(lead.getLastContactAt()); verifyNoInteractions(leads);
    }
    @Test void completedTaskClearedAndLogged() {
        lead.updateWork("HIGH", "Позвонить", Instant.now().plusSeconds(100), false);
        service.update(id, actor, base().version(1L).operation(LeadWorkCommand.Operation.COMPLETE).build());
        assertNull(lead.getNextAction()); assertNull(lead.getNextActionAt());
        verify(history).logWork(eq(lead), eq(actor), contains("Выполнено: Позвонить"));
    }
    @Test void noTaskCannotComplete() {
        assertThrows(BadRequestException.class, () -> service.update(id, actor, base().operation(LeadWorkCommand.Operation.COMPLETE).build()));
    }
    @Test void noAnswerDoesNotChangeStage() {
        service.update(id, actor, base().operation(LeadWorkCommand.Operation.CONTACT)
                .channel(LeadWorkCommand.Channel.PHONE).outcome(LeadWorkCommand.Outcome.NO_ANSWER).build());
        verifyNoInteractions(leads); assertEquals(LeadStatus.NEW, lead.getStatus());
    }
    @Test void reachedNewLeadTransitionsAtomically() {
        service.update(id, actor, base().operation(LeadWorkCommand.Operation.CONTACT)
                .channel(LeadWorkCommand.Channel.PHONE).outcome(LeadWorkCommand.Outcome.REACHED).build());
        verify(leads).processEvent(id, LeadEvent.CONTACT, null, null, actor);
    }
    @Test void missingOutcomeRejected() {
        assertThrows(BadRequestException.class, () -> service.update(id, actor, base().operation(LeadWorkCommand.Operation.CONTACT).channel(LeadWorkCommand.Channel.PHONE).build()));
        verifyNoInteractions(history);
    }
    @Test void terminalLeadCannotBeChanged() {
        for (LeadStatus status : List.of(LeadStatus.CONVERTED, LeadStatus.LOST)) {
            lead.updateStatus(status);
            assertThrows(BadRequestException.class, () -> service.update(id, actor, base().build()));
        }
    }
}
