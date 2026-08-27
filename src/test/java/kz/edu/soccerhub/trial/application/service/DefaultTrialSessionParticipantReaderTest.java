package kz.edu.soccerhub.trial.application.service;

import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;
import kz.edu.soccerhub.common.dto.trial.TrialSessionParticipantDto;
import kz.edu.soccerhub.common.port.TrialLeadPort;
import kz.edu.soccerhub.common.port.TrialStudentDetailsPort;
import kz.edu.soccerhub.trial.application.service.impl.DefaultTrialSessionParticipantReader;
import kz.edu.soccerhub.trial.domain.entity.TrialBooking;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultTrialSessionParticipantReaderTest {

    @Mock
    private TrialStudentDetailsPort studentPort;

    @Mock
    private TrialLeadPort leadPort;

    private DefaultTrialSessionParticipantReader reader;

    @BeforeEach
    void setUp() {
        reader = new DefaultTrialSessionParticipantReader(
                studentPort,
                leadPort
        );
    }

    @Test
    void resolvesExistingStudentAndLeadParticipant() {
        UUID sessionId = UUID.randomUUID();

        UUID studentId = UUID.randomUUID();
        TrialBooking studentBooking = TrialBooking.schedule(
                null,
                null,
                null,
                studentId,
                sessionId
        );

        UUID leadId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        TrialBooking participantBooking = TrialBooking.schedule(
                leadId,
                null,
                participantId,
                null,
                sessionId
        );

        TrialBookingDetailsDto.Student student =
                TrialBookingDetailsDto.Student.builder()
                        .id(studentId)
                        .fullName("Existing Student")
                        .birthDate(LocalDate.of(2015, 4, 10))
                        .age(11)
                        .build();

        TrialBookingDetailsDto.Student participant =
                TrialBookingDetailsDto.Student.builder()
                        .id(participantId)
                        .fullName("Lead Participant")
                        .birthDate(LocalDate.of(2017, 8, 20))
                        .age(8)
                        .build();

        when(studentPort.getDetails(Set.of(studentId)))
                .thenReturn(Map.of(studentId, student));

        when(leadPort.getParticipantDetails(
                Set.of(participantId)
        )).thenReturn(Map.of(participantId, participant));

        List<TrialSessionParticipantDto> result = reader.read(
                List.of(studentBooking, participantBooking)
        );

        assertEquals(2, result.size());

        TrialSessionParticipantDto existingStudent =
                result.get(0);

        assertEquals(
                studentBooking.getId(),
                existingStudent.trialBookingId()
        );
        assertEquals(studentId, existingStudent.studentId());
        assertEquals(
                "Existing Student",
                existingStudent.fullName()
        );
        assertEquals(
                LocalDate.of(2015, 4, 10),
                existingStudent.birthDate()
        );

        TrialSessionParticipantDto leadParticipant =
                result.get(1);

        assertEquals(
                participantBooking.getId(),
                leadParticipant.trialBookingId()
        );
        assertEquals(leadId, leadParticipant.leadId());
        assertEquals(
                participantId,
                leadParticipant.participantId()
        );
        assertEquals(
                "Lead Participant",
                leadParticipant.fullName()
        );
        assertEquals(
                LocalDate.of(2017, 8, 20),
                leadParticipant.birthDate()
        );
    }

    @Test
    void returnsEmptyListWithoutCallingForeignPorts() {
        assertEquals(List.of(), reader.read(List.of()));

        verifyNoInteractions(studentPort, leadPort);
    }
}