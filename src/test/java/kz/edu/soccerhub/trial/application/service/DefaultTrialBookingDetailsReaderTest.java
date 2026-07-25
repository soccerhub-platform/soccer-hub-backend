package kz.edu.soccerhub.trial.application.service;

import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;
import kz.edu.soccerhub.common.dto.trial.TrialSessionContext;
import kz.edu.soccerhub.common.port.TrialSessionPort;
import kz.edu.soccerhub.common.port.TrialStudentDetailsPort;
import kz.edu.soccerhub.common.port.TrialLeadPort;
import kz.edu.soccerhub.common.port.TrialGroupPort;
import kz.edu.soccerhub.common.port.TrialCoachPort;
import kz.edu.soccerhub.common.port.TrialLocationPort;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus;
import kz.edu.soccerhub.trial.domain.entity.TrialBooking;
import kz.edu.soccerhub.trial.domain.enums.TrialAttendanceStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialBookingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class DefaultTrialBookingDetailsReaderTest {

    @Mock
    private TrialStudentDetailsPort studentDetailsPort;

    @Mock
    private TrialSessionPort sessionPort;

    @Mock
    private TrialLeadPort leadPort;

    @Mock
    private TrialGroupPort groupPort;

    @Mock
    private TrialCoachPort coachPort;

    @Mock
    private TrialLocationPort locationPort;

    private DefaultTrialBookingDetailsReader reader;

    private UUID studentId;
    private UUID sessionId;

    @BeforeEach
    void setUp() {
        reader = new DefaultTrialBookingDetailsReader(
                studentDetailsPort,
                sessionPort,
                leadPort,
                groupPort,
                coachPort,
                locationPort
        );

        studentId = UUID.randomUUID();
        sessionId = UUID.randomUUID();

        when(studentDetailsPort.getDetails(studentId))
                .thenReturn(
                        TrialBookingDetailsDto.Student.builder()
                                .id(studentId)
                                .fullName("Test Student")
                                .birthDate(LocalDate.of(2015, 1, 1))
                                .age(11)
                                .build()
                );

        LocalDateTime startsAt =
                LocalDateTime.now().plusDays(1);

        when(sessionPort.getSessionDetails(sessionId))
                .thenReturn(
                        TrialSessionContext.builder()
                                .sessionId(sessionId)
                                .groupId(UUID.randomUUID())
                                .coachId(UUID.randomUUID())
                                .locationId(UUID.randomUUID())
                                .startsAt(startsAt)
                                .endsAt(startsAt.plusHours(1))
                                .status(TrainingSessionStatus.PLANNED)
                                .build()
                );

        when(groupPort.getDetails(any(UUID.class)))
                .thenReturn(
                        TrialBookingDetailsDto.Group.builder()
                                .id(UUID.randomUUID())
                                .name("Test Group")
                                .build()
                );

        when(coachPort.getDetails(any(UUID.class)))
                .thenReturn(
                        TrialBookingDetailsDto.Coach.builder()
                                .id(UUID.randomUUID())
                                .fullName("Test Coach")
                                .build()
                );

        when(locationPort.getDetails(any(UUID.class)))
                .thenReturn(
                        TrialBookingDetailsDto.Location.builder()
                                .id(UUID.randomUUID())
                                .name("Main Field")
                                .build()
                );
    }

    @Test
    void scheduledTrialHasScheduledCapabilities() {
        TrialBooking booking = createBooking();

        TrialBookingDetailsDto details = reader.read(booking);

        assertTrue(details.capabilities().canConfirm());
        assertTrue(details.capabilities().canCancel());
        assertTrue(details.capabilities().canReschedule());
        assertTrue(details.capabilities().canMarkAttendance());
        assertFalse(details.capabilities().canRecordResult());
    }

    @Test
    void confirmedTrialCannotBeConfirmedAgain() {
        TrialBooking booking = createBooking();
        booking.confirm();

        TrialBookingDetailsDto details = reader.read(booking);

        assertFalse(details.capabilities().canConfirm());
        assertTrue(details.capabilities().canCancel());
        assertTrue(details.capabilities().canReschedule());
        assertTrue(details.capabilities().canMarkAttendance());
        assertFalse(details.capabilities().canRecordResult());
    }

    @Test
    void completedTrialCanRecordResult() {
        TrialBooking booking = createBooking();

        booking.confirm();
        booking.markAttendance(
                TrialAttendanceStatus.ATTENDED,
                UUID.randomUUID(),
                "Good trial"
        );

        TrialBookingDetailsDto details = reader.read(booking);

        assertFalse(details.capabilities().canConfirm());
        assertFalse(details.capabilities().canCancel());
        assertFalse(details.capabilities().canReschedule());
        assertFalse(details.capabilities().canMarkAttendance());
        assertTrue(details.capabilities().canRecordResult());
    }

    @Test
    void canceledTrialHasNoActions() {
        TrialBooking booking = createBooking();

        booking.cancel("Student cancelled");

        TrialBookingDetailsDto details = reader.read(booking);

        assertFalse(details.capabilities().canConfirm());
        assertFalse(details.capabilities().canCancel());
        assertFalse(details.capabilities().canReschedule());
        assertFalse(details.capabilities().canMarkAttendance());
        assertFalse(details.capabilities().canRecordResult());
    }

    private TrialBooking createBooking() {
        return TrialBooking.schedule(
                UUID.randomUUID(),
                UUID.randomUUID(),
                studentId,
                sessionId
        );
    }

}
