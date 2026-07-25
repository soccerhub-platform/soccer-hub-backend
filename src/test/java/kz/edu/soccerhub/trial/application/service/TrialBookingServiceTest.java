package kz.edu.soccerhub.trial.application.service;

import kz.edu.soccerhub.common.dto.trial.CreateTrialBookingCommand;
import kz.edu.soccerhub.common.dto.trial.CancelTrialCommand;
import kz.edu.soccerhub.common.dto.trial.MarkTrialAttendanceCommand;
import kz.edu.soccerhub.common.dto.trial.RecordTrialResultCommand;
import kz.edu.soccerhub.common.dto.trial.TrialSessionContext;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.port.TrialGroupPort;
import kz.edu.soccerhub.common.port.TrialLeadPort;
import kz.edu.soccerhub.common.port.TrialSessionPort;
import kz.edu.soccerhub.common.port.TrialStudentPort;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialBookingStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialAttendanceStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialResult;
import kz.edu.soccerhub.trial.domain.entity.TrialBooking;
import kz.edu.soccerhub.trial.domain.repository.TrialBookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrialBookingServiceTest {

    @Mock
    private TrialBookingRepository repository;
    @Mock
    private TrialGroupPort groupPort;
    @Mock
    private TrialSessionPort sessionPort;
    @Mock
    private TrialStudentPort studentPort;
    @Mock
    private TrialLeadPort leadPort;
    @Mock
    private TrialBookingDetailsReader detailsReader;

    private TrialBookingService service;

    @BeforeEach
    void setUp() {
        service = new TrialBookingService(
                repository,
                groupPort,
                sessionPort,
                studentPort,
                leadPort,
                detailsReader
        );
    }

    @Test
    void createsTrialForBookableSession() {
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();

        when(sessionPort.getBookableSession(sessionId))
                .thenReturn(sessionContext(sessionId, groupId));
        when(repository.existsByStudentIdAndTrainingSessionIdAndStatusIn(
                studentId,
                sessionId,
                List.of(TrialBookingStatus.SCHEDULED, TrialBookingStatus.CONFIRMED)
        )).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.createTrial(command(leadId, studentId, sessionId));

        assertEquals(studentId, result.studentId());
        assertEquals(sessionId, result.trainingSessionId());
        assertEquals(TrialBookingStatus.SCHEDULED, result.status());
        verify(leadPort).validateParticipant(leadId, studentId);
        verify(groupPort).validateAvailableCapacity(eq(groupId), eq(studentId), any());
    }

    @Test
    void rejectsDuplicateActiveTrial() {
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(sessionPort.getBookableSession(sessionId))
                .thenReturn(sessionContext(sessionId, UUID.randomUUID()));
        when(repository.existsByStudentIdAndTrainingSessionIdAndStatusIn(
                eq(studentId),
                eq(sessionId),
                any()
        )).thenReturn(true);

        assertThrows(
                BadRequestException.class,
                () -> service.createTrial(command(null, studentId, sessionId))
        );

        verify(repository, never()).save(any());
    }

    @Test
    void propagatesGroupCapacityValidation() {
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        when(sessionPort.getBookableSession(sessionId))
                .thenReturn(sessionContext(sessionId, groupId));
        doThrow(new BadRequestException("Group has no available places"))
                .when(groupPort)
                .validateAvailableCapacity(eq(groupId), eq(studentId), any());

        assertThrows(
                BadRequestException.class,
                () -> service.createTrial(command(null, studentId, sessionId))
        );

        verify(repository, never()).save(any());
    }

    @Test
    void confirmsScheduledTrial() {
        TrialBooking booking = createBooking();
        UUID adminId = UUID.randomUUID();

        when(repository.findById(booking.getId()))
                .thenReturn(Optional.of(booking));

        service.confirmTrial(booking.getId(), adminId);

        assertEquals(TrialBookingStatus.CONFIRMED, booking.getStatus());
        verify(detailsReader).read(booking);
    }

    @Test
    void cancelsTrialWithReason() {
        TrialBooking booking = createBooking();
        UUID adminId = UUID.randomUUID();

        when(repository.findById(booking.getId()))
                .thenReturn(Optional.of(booking));

        service.cancelTrial(
                CancelTrialCommand.builder()
                        .trialId(booking.getId())
                        .adminId(adminId)
                        .reason("Student unavailable")
                        .build()
        );

        assertEquals(TrialBookingStatus.CANCELED, booking.getStatus());
        assertEquals("Student unavailable", booking.getCancellationReason());
    }

    @Test
    void marksAttendanceAndCompletesTrial() {
        TrialBooking booking = createBooking();
        UUID adminId = UUID.randomUUID();

        when(repository.findById(booking.getId()))
                .thenReturn(Optional.of(booking));

        service.markAttendance(
                MarkTrialAttendanceCommand.builder()
                        .trialId(booking.getId())
                        .adminId(adminId)
                        .status(TrialAttendanceStatus.ATTENDED)
                        .comment("Attended trial")
                        .build()
        );

        assertEquals(TrialBookingStatus.COMPLETED, booking.getStatus());
        assertEquals(TrialAttendanceStatus.ATTENDED, booking.getAttendanceStatus());
    }

    @Test
    void recordsResultOnlyAfterAttendance() {
        TrialBooking booking = createBooking();
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        booking.confirm();
        booking.markAttendance(
                TrialAttendanceStatus.ATTENDED,
                adminId,
                null
        );

        when(repository.findById(booking.getId()))
                .thenReturn(Optional.of(booking));

        service.recordResult(
                RecordTrialResultCommand.builder()
                        .trialId(booking.getId())
                        .adminId(adminId)
                        .result(TrialResult.INTERESTED)
                        .recommendedGroupId(groupId)
                        .coachFeedback("Ready for group")
                        .build()
        );

        assertEquals(TrialResult.INTERESTED, booking.getResult());
        assertEquals(groupId, booking.getRecommendedGroupId());
    }

    private CreateTrialBookingCommand command(
            UUID leadId,
            UUID studentId,
            UUID sessionId
    ) {
        return CreateTrialBookingCommand.builder()
                .leadId(leadId)
                .participantId(studentId)
                .studentId(studentId)
                .trainingSessionId(sessionId)
                .adminId(UUID.randomUUID())
                .build();
    }

    private TrialSessionContext sessionContext(UUID sessionId, UUID groupId) {
        LocalDateTime startsAt = LocalDateTime.now().plusDays(1);

        return TrialSessionContext.builder()
                .sessionId(sessionId)
                .groupId(groupId)
                .coachId(UUID.randomUUID())
                .startsAt(startsAt)
                .endsAt(startsAt.plusHours(1))
                .status(TrainingSessionStatus.PLANNED)
                .build();
    }

    private TrialBooking createBooking() {
        return TrialBooking.schedule(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );
    }
}
