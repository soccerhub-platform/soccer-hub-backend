package kz.edu.soccerhub.crm.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.edu.soccerhub.common.dto.client.ClientConversionOutput;
import kz.edu.soccerhub.common.dto.client.ClientStudentRelationshipType;
import kz.edu.soccerhub.common.dto.lead.ConvertLeadRequest;
import kz.edu.soccerhub.common.dto.lead.ConvertLeadResponse;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.port.ClientPort;
import kz.edu.soccerhub.crm.domain.model.Lead;
import kz.edu.soccerhub.crm.domain.model.LeadParticipant;
import kz.edu.soccerhub.crm.domain.model.enums.Gender;
import kz.edu.soccerhub.crm.domain.model.enums.LeadSource;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.crm.domain.model.enums.LeadType;
import kz.edu.soccerhub.crm.domain.repository.LeadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeadServiceConversionTest {

    @Mock
    private LeadRepository leadRepository;
    @Mock
    private ClientPort clientPort;
    @Mock
    private LeadActivityService leadActivityService;

    private DefaultLeadConversionService conversionService;

    @BeforeEach
    void setUp() {
        conversionService = new DefaultLeadConversionService(
                leadRepository,
                clientPort,
                leadActivityService,
                new ObjectMapper()
        );
    }

    @Test
    void conversionCreatesClientStudentRelationOnly() {
        UUID leadId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID relationId = UUID.randomUUID();
        Lead lead = lead(leadId, LeadStatus.DECISION_PENDING, participantId, "Alex Doe");
        ConvertLeadRequest request = new ConvertLeadRequest(
                participantId,
                LocalDate.of(2016, 5, 20),
                ClientStudentRelationshipType.MOTHER,
                false,
                false
        );

        when(leadRepository.findById(leadId)).thenReturn(java.util.Optional.of(lead));
        when(clientPort.convertLead(any())).thenReturn(new ClientConversionOutput(clientId, playerId, relationId));

        ConvertLeadResponse response = conversionService.convertLeadToClient(leadId, request, UUID.randomUUID());

        assertEquals(LeadStatus.CONVERTED, response.leadStatus());
        assertEquals(clientId, response.clientId());
        assertEquals(playerId, response.playerId());
        assertEquals(LeadStatus.CONVERTED, lead.getStatus());
        verify(leadRepository).save(lead);
    }

    @Test
    void conversionRejectsClosedLead() {
        UUID leadId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        when(leadRepository.findById(leadId)).thenReturn(java.util.Optional.of(
                lead(leadId, LeadStatus.LOST, participantId, "Alex Doe")
        ));

        assertThrows(BadRequestException.class, () -> conversionService.convertLeadToClient(
                leadId,
                new ConvertLeadRequest(participantId, LocalDate.of(2016, 5, 20), ClientStudentRelationshipType.MOTHER, false, false),
                UUID.randomUUID()
        ));
    }

    private Lead lead(UUID leadId, LeadStatus status, UUID participantId, String participantName) {
        Lead lead = Lead.builder()
                .id(leadId)
                .leadType(LeadType.CHILDREN)
                .primaryContactName("Parent One")
                .primaryContactPhone("+77001112233")
                .primaryContactEmail("parent@example.com")
                .source(LeadSource.OTHER)
                .status(status)
                .branchId(UUID.randomUUID())
                .build();
        lead.getParticipants().add(LeadParticipant.builder()
                .id(participantId)
                .lead(lead)
                .fullName(participantName)
                .birthDate(LocalDate.of(2016, 5, 20))
                .gender(Gender.MALE)
                .build());
        return lead;
    }
}
