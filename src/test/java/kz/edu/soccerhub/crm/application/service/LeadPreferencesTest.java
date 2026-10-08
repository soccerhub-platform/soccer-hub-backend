package kz.edu.soccerhub.crm.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.edu.soccerhub.common.dto.lead.LeadPreferencesCommand;
import kz.edu.soccerhub.common.exception.*;
import kz.edu.soccerhub.crm.domain.model.Lead;
import kz.edu.soccerhub.crm.domain.model.enums.*;
import kz.edu.soccerhub.crm.domain.repository.LeadRepository;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LeadPreferencesTest {
    LeadRepository repo = mock(LeadRepository.class);
    LeadActivityService history = mock(LeadActivityService.class);
    LeadService leads = mock(LeadService.class);
    ObjectMapper mapper = new ObjectMapper();
    LeadWorkService service = new LeadWorkService(repo, history, leads, mapper);
    UUID id = UUID.randomUUID(), actor = UUID.randomUUID();
    Lead lead = Lead.builder().id(id).status(LeadStatus.TRIAL_SCHEDULED).build();
    LeadPreferencesTest() { when(repo.findLockedById(id)).thenReturn(Optional.of(lead)); }
    LeadPreferencesCommand.LeadPreferencesCommandBuilder command() {
        return LeadPreferencesCommand.builder().version(0L).preferredDays(List.of("FRI", "MON", "MON"))
                .timePreference(TimePreference.EVENING).experience("BEGINNER").notes(" Район центра ");
    }
    @Test void savesWithoutChangingStageParticipantsOwnerOrTrial() {
        lead.addParticipant("Ребёнок", LocalDate.of(2015,1,1), Gender.MALE, "BEGINNER");
        var participant = lead.getParticipants().getFirst();
        service.updatePreferences(id, actor, command().build());
        assertEquals("MON,FRI", lead.getPreferredDays());
        assertEquals("Район центра", lead.getNotes());
        assertEquals(LeadStatus.TRIAL_SCHEDULED, lead.getStatus());
        assertSame(participant, lead.getParticipants().getFirst());
        assertNull(lead.getAssignedAdminId());
        assertEquals(1, lead.getWorkVersion());
        verifyNoInteractions(leads);
        verify(history).logWork(eq(lead), eq(actor), contains("Пожелания"));
    }
    @Test void allActiveStagesAllowPreferences() {
        for (LeadStatus status : LeadStatus.values()) {
            if (status == LeadStatus.LOST || status == LeadStatus.CONVERTED) continue;
            lead.updateStatus(status);
            service.updatePreferences(id, actor, command().version(lead.getWorkVersion()).build());
            assertEquals(status, lead.getStatus());
        }
    }
    @Test void clearingAlsoClearsLegacyReadModel() throws Exception {
        lead.updateQualificationData("{\"preferredDays\":\"MON\",\"notes\":\"old\",\"timePreference\":\"EVENING\",\"participants\":[{\"fullName\":\"keep\"}]}");
        service.updatePreferences(id, actor, command().preferredDays(List.of()).timePreference(null).experience(null).notes(null).build());
        var json = mapper.readTree(lead.getQualificationData());
        assertEquals("", lead.getPreferredDays()); assertNull(lead.getTimePreference());
        assertEquals("", json.get("notes").asText()); assertTrue(json.get("timePreference").isNull());
        assertEquals("keep", json.get("participants").get(0).get("fullName").asText());
    }
    @Test void preservesExistingTaskAndContact() {
        var due = Instant.now().minusSeconds(100);
        lead.updateWork("HIGH", "Связаться", due, true);
        var contact = lead.getLastContactAt();
        service.updatePreferences(id, actor, command().version(1L).build());
        assertEquals(due, lead.getNextActionAt()); assertEquals("Связаться", lead.getNextAction());
        assertEquals(contact, lead.getLastContactAt()); assertEquals("HIGH", lead.getWorkPriority());
    }
    @Test void staleVersionDoesNotOverwrite() {
        assertThrows(ConflictException.class, () -> service.updatePreferences(id, actor, command().version(8L).build()));
        verifyNoInteractions(history); assertNull(lead.getPreferredDays());
    }
    @Test void closedLeadIsReadOnly() {
        for (var status : List.of(LeadStatus.LOST, LeadStatus.CONVERTED)) {
            lead.updateStatus(status);
            assertThrows(BadRequestException.class, () -> service.updatePreferences(id, actor, command().build()));
        }
        verifyNoInteractions(history);
    }
    @Test void invalidDaysRejected() {
        assertThrows(BadRequestException.class, () -> service.updatePreferences(id, actor, command().preferredDays(List.of("INVALID")).build()));
        verifyNoInteractions(history);
    }
    @Test void oversizedNotesRejected() {
        assertThrows(BadRequestException.class, () -> service.updatePreferences(id, actor, command().notes("x".repeat(1001)).build()));
        verifyNoInteractions(history);
    }
}
