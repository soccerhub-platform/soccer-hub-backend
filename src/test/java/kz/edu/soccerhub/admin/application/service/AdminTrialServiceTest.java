package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.trial.AdminMarkTrialAttendanceInput;
import kz.edu.soccerhub.admin.application.dto.trial.AdminRescheduleTrialInput;
import kz.edu.soccerhub.admin.application.dto.trial.AdminTrialDetailsOutput;
import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;
import kz.edu.soccerhub.common.port.LeadPort;
import kz.edu.soccerhub.common.port.TrialPort;
import kz.edu.soccerhub.crm.application.state.LeadEvent;
import kz.edu.soccerhub.trial.domain.enums.TrialAttendanceStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialBookingStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import kz.edu.soccerhub.admin.application.dto.trial.CreateTrialBookingInput;
import kz.edu.soccerhub.common.dto.trial.TrialBookingDto;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminTrialServiceTest {

    @Mock
    private TrialPort trialPort;

    @Mock
    private LeadPort leadPort;

    @InjectMocks
    private AdminTrialService service;

    @Test
    void createShouldScheduleLeadWithoutMutatingParticipantStage() {
        UUID adminId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID trainingSessionId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();

        CreateTrialBookingInput input = CreateTrialBookingInput.builder()
                .leadId(leadId)
                .participantId(participantId)
                .trainingSessionId(trainingSessionId)
                .build();

        when(trialPort.createTrial(any()))
                .thenReturn(TrialBookingDto.builder()
                        .id(bookingId)
                        .leadId(leadId)
                        .participantId(participantId)
                        .trainingSessionId(trainingSessionId)
                        .status(TrialBookingStatus.SCHEDULED)
                        .build());

        service.create(adminId, input);

        verify(leadPort).processEvent(
                leadId,
                LeadEvent.SCHEDULE_TRIAL,
                null,
                null,
                adminId
        );
    }

    @Test
    void markAttendanceShouldCompleteLeadWhenStudentAttended() {
        UUID trialId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        when(trialPort.markAttendance(any()))
                .thenReturn(details(leadId, TrialAttendanceStatus.ATTENDED));

        AdminTrialDetailsOutput output = service.markAttendance(
                trialId,
                adminId,
                new AdminMarkTrialAttendanceInput(
                        TrialAttendanceStatus.ATTENDED,
                        "Посетил пробное"
                )
        );

        verify(leadPort).processEvent(
                leadId,
                LeadEvent.COMPLETE_TRIAL,
                null,
                null,
                adminId
        );

        verify(trialPort).markAttendance(any());
    }

    @Test
    void markAttendanceShouldMarkLeadNoShowWhenStudentDidNotAttend() {
        UUID trialId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        when(trialPort.markAttendance(any()))
                .thenReturn(details(leadId, TrialAttendanceStatus.NO_SHOW));

        service.markAttendance(
                trialId,
                adminId,
                new AdminMarkTrialAttendanceInput(
                        TrialAttendanceStatus.NO_SHOW,
                        "Не пришёл"
                )
        );

        verify(leadPort).processEvent(
                leadId,
                LeadEvent.NO_SHOW,
                null,
                null,
                adminId
        );
    }

    @Test
    void markAttendanceShouldNotProcessLeadWhenTrialHasNoLead() {
        UUID trialId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        when(trialPort.markAttendance(any()))
                .thenReturn(details(null, TrialAttendanceStatus.ATTENDED));

        service.markAttendance(
                trialId,
                adminId,
                new AdminMarkTrialAttendanceInput(
                        TrialAttendanceStatus.ATTENDED,
                        null
                )
        );

        verifyNoInteractions(leadPort);
    }

    @Test
    void cancelShouldReturnLinkedLeadToInProgress() {
        UUID trialId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        when(trialPort.cancelTrial(any()))
                .thenReturn(canceledDetails(leadId));

        service.cancel(
                trialId,
                adminId,
                "Ребёнок не сможет прийти"
        );

        verify(trialPort).cancelTrial(any());

        verify(leadPort).processEvent(
                leadId,
                LeadEvent.CANCEL_TRIAL,
                null,
                null,
                adminId
        );
    }

    @Test
    void cancelShouldNotProcessLeadWhenTrialHasNoLead() {
        UUID trialId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        when(trialPort.cancelTrial(any()))
                .thenReturn(canceledDetails(null));

        service.cancel(
                trialId,
                adminId,
                "Ребёнок не сможет прийти"
        );

        verify(trialPort).cancelTrial(any());
        verifyNoInteractions(leadPort);
    }

    @Test
    void rescheduleShouldDelegateToTrialModule() {
        UUID trialId = UUID.randomUUID();
        UUID trainingSessionId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        when(trialPort.rescheduleTrial(any()))
                .thenReturn(scheduledDetails());

        service.reschedule(
                trialId,
                adminId,
                new AdminRescheduleTrialInput(trainingSessionId)
        );

        verify(trialPort).rescheduleTrial(argThat(command ->
                trialId.equals(command.trialId())
                        && trainingSessionId.equals(
                        command.trainingSessionId()
                )
                        && adminId.equals(command.adminId())
        ));

        verifyNoInteractions(leadPort);
    }

    private TrialBookingDetailsDto details(
            UUID leadId,
            TrialAttendanceStatus attendanceStatus
    ) {
        return TrialBookingDetailsDto.builder()
                .id(UUID.randomUUID())
                .status(TrialBookingStatus.COMPLETED)
                .attendanceStatus(attendanceStatus)
                .result(TrialResult.PENDING)
                .lead(leadId == null ? null : TrialBookingDetailsDto.Lead.builder()
                                              .id(leadId)
                                              .fullName("Test Lead")
                                              .build())
                .build();
    }

    private TrialBookingDetailsDto canceledDetails(UUID leadId) {
        return TrialBookingDetailsDto.builder()
                .id(UUID.randomUUID())
                .status(TrialBookingStatus.CANCELED)
                .attendanceStatus(TrialAttendanceStatus.UNMARKED)
                .result(TrialResult.PENDING)
                .lead(leadId == null
                        ? null
                        : TrialBookingDetailsDto.Lead.builder()
                        .id(leadId)
                        .fullName("Test Lead")
                        .build())
                .build();
    }

    private TrialBookingDetailsDto scheduledDetails() {
        return TrialBookingDetailsDto.builder()
                .id(UUID.randomUUID())
                .status(TrialBookingStatus.SCHEDULED)
                .attendanceStatus(TrialAttendanceStatus.UNMARKED)
                .result(TrialResult.PENDING)
                .build();
    }
}