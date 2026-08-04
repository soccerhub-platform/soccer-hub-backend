package kz.edu.soccerhub.crm.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import kz.edu.soccerhub.common.dto.lead.LeadCreateCommand;
import kz.edu.soccerhub.common.dto.lead.LeadParticipantInput;
import kz.edu.soccerhub.common.dto.lead.LeadPrimaryContactInput;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.port.AdminPort;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.common.port.GroupSchedulePort;
import kz.edu.soccerhub.crm.application.mapper.LeadMapper;
import kz.edu.soccerhub.crm.application.state.LeadStateMachineService;
import kz.edu.soccerhub.crm.domain.model.Lead;
import kz.edu.soccerhub.crm.domain.model.enums.Gender;
import kz.edu.soccerhub.crm.domain.model.enums.LeadType;
import kz.edu.soccerhub.crm.domain.repository.LeadLossReasonRepository;
import kz.edu.soccerhub.crm.domain.repository.LeadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceCreationTest {

    @Mock private LeadRepository leadRepository;
    @Mock private LeadStateMachineService stateMachineService;
    @Mock private AdminPort adminPort;
    @Mock private GroupPort groupPort;
    @Mock private CoachPort coachPort;
    @Mock private GroupSchedulePort groupSchedulePort;
    @Mock private LeadConversionService leadConversionService;
    @Mock private LeadActivityService leadActivityService;
    @Mock private LeadMapper leadMapper;
    @Mock private LeadLossReasonRepository leadLossReasonRepository;
    @Mock private LeadLossReasonPolicy leadLossReasonPolicy;

    private LeadService leadService;

    @BeforeEach
    void setUp() {
        leadService = new LeadService(
                leadRepository,
                stateMachineService,
                adminPort,
                groupPort,
                coachPort,
                groupSchedulePort,
                leadConversionService,
                leadActivityService,
                leadMapper,
                leadLossReasonRepository,
                leadLossReasonPolicy,
                new ObjectMapper()
        );
    }

    @Test
    void createsSeparateLeadForEachParticipant() {
        LeadCreateCommand command = command(List.of(
                participant("  Алихан   Сарсенов  ", LocalDate.of(2017, 5, 10)),
                participant("Диас Сарсенов", LocalDate.of(2019, 8, 15))
        ));

        List<UUID> leadIds = leadService.createLeads(command);

        assertEquals(2, leadIds.size());
        assertNotEquals(leadIds.get(0), leadIds.get(1));

        ArgumentCaptor<Lead> captor = ArgumentCaptor.forClass(Lead.class);
        verify(leadRepository, times(2)).save(captor.capture());
        verify(leadActivityService, times(2)).logLeadCreated(any());

        List<Lead> leads = captor.getAllValues();

        assertEquals(1, leads.get(0).getParticipants().size());
        assertEquals(1, leads.get(1).getParticipants().size());
        assertEquals(
                "Алихан Сарсенов",
                leads.get(0).getParticipants().getFirst().getFullName()
        );
        assertEquals(
                "Диас Сарсенов",
                leads.get(1).getParticipants().getFirst().getFullName()
        );
    }

    @Test
    void rejectsDuplicatedParticipantBeforeSavingAnything() {
        LeadCreateCommand command = command(List.of(
                participant("Алихан Сарсенов", LocalDate.of(2017, 5, 10)),
                participant("  алихан   сарсенов ", LocalDate.of(2017, 5, 10))
        ));

        assertThrows(
                BadRequestException.class,
                () -> leadService.createLeads(command)
        );

        verify(leadRepository, never()).save(any());
        verify(leadActivityService, never()).logLeadCreated(any());
    }

    @Test
    void rejectsEmptyParticipantList() {
        LeadCreateCommand command = command(List.of());

        assertThrows(
                BadRequestException.class,
                () -> leadService.createLeads(command)
        );

        verify(leadRepository, never()).save(any());
        verify(leadActivityService, never()).logLeadCreated(any());
    }

    @Test
    void rejectsExistingActiveLeadForSameParticipant() {
        LocalDate birthDate = LocalDate.of(2017, 5, 10);

        when(leadRepository.existsActiveLead(
                "+77001234567",
                "алихан сарсенов",
                birthDate
        )).thenReturn(true);

        LeadCreateCommand command = command(List.of(
                participant("Алихан Сарсенов", birthDate)
        ));

        assertThrows(
                BadRequestException.class,
                () -> leadService.createLeads(command)
        );

        verify(leadRepository, never()).save(any());
        verify(leadActivityService, never()).logLeadCreated(any());
    }

    private LeadCreateCommand command(
            List<LeadParticipantInput> participants
    ) {
        return new LeadCreateCommand(
                LeadType.CHILDREN,
                new LeadPrimaryContactInput(
                        "Айгуль Сарсенова",
                        "+7 700 123 45 67",
                        "parent@example.com"
                ),
                "Два ребёнка",
                null,
                UUID.randomUUID(),
                participants
        );
    }

    private LeadParticipantInput participant(
            String name,
            LocalDate birthDate
    ) {
        return new LeadParticipantInput(
                name,
                birthDate,
                Gender.MALE,
                "BEGINNER"
        );
    }
}