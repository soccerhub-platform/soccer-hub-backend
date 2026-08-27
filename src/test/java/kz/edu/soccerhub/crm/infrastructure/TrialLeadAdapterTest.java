package kz.edu.soccerhub.crm.infrastructure;

import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;
import kz.edu.soccerhub.crm.domain.model.Lead;
import kz.edu.soccerhub.crm.domain.model.enums.Gender;
import kz.edu.soccerhub.crm.domain.repository.LeadRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrialLeadAdapterTest {

    @Test
    void resolvesLeadParticipantsByParticipantIds() {
        LeadRepository repository = mock(LeadRepository.class);
        TrialLeadAdapter adapter = new TrialLeadAdapter(repository);
        LocalDate birthDate = LocalDate.now().minusYears(9).minusDays(1);
        Lead lead = Lead.builder()
                .id(UUID.randomUUID())
                .build();
        lead.addParticipant(
                "Ермахан А.",
                birthDate,
                Gender.MALE,
                null
        );
        UUID participantId = lead.getParticipants().getFirst().getId();

        when(repository.findByLeadParticipantIdInOrderByUpdatedAtDesc(
                Set.of(participantId)
        )).thenReturn(List.of(lead));

        Map<UUID, TrialBookingDetailsDto.Student> result =
                adapter.getParticipantDetails(Set.of(participantId));

        assertEquals("Ермахан А.", result.get(participantId).fullName());
        assertEquals(birthDate, result.get(participantId).birthDate());
        assertEquals(9, result.get(participantId).age());
    }
}
