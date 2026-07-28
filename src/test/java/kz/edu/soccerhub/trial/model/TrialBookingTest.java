package kz.edu.soccerhub.trial.model;

import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.trial.domain.entity.TrialBooking;
import kz.edu.soccerhub.trial.domain.enums.TrialAttendanceStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialNextActionType;
import kz.edu.soccerhub.trial.domain.enums.TrialResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TrialBookingTest {

    @Test
    void shouldNotRecordResultForNoShow() {
        TrialBooking booking = TrialBooking.schedule(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        booking.markAttendance(
                TrialAttendanceStatus.NO_SHOW,
                UUID.randomUUID(),
                "Не пришёл"
        );

        assertThrows(
                BadRequestException.class,
                () -> booking.recordResult(
                        TrialResult.NOT_INTERESTED,
                        null,
                        null,
                        null,
                        null
                )
        );
    }

    @Test
    void shouldRecordResultForAttendedTrial() {
        TrialBooking booking = TrialBooking.schedule(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        booking.markAttendance(
                TrialAttendanceStatus.ATTENDED,
                UUID.randomUUID(),
                "Посетил занятие"
        );

        booking.recordResult(
                TrialResult.INTERESTED,
                null,
                "Хороший уровень",
                null,
                null
        );

        assertEquals(TrialResult.INTERESTED, booking.getResult());
    }

    @Test
    void shouldStoreFollowUpActionForAttendedTrial() {
        TrialBooking booking = TrialBooking.schedule(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        booking.markAttendance(
                TrialAttendanceStatus.ATTENDED,
                UUID.randomUUID(),
                "Посетил занятие"
        );

        LocalDateTime dueAt = LocalDateTime.now().plusDays(2);

        booking.recordResult(
                TrialResult.FOLLOW_UP,
                null,
                "Нужно перезвонить",
                TrialNextActionType.CALL,
                dueAt
        );

        assertEquals(TrialResult.FOLLOW_UP, booking.getResult());
        assertEquals(TrialNextActionType.CALL, booking.getNextActionType());
        assertEquals(dueAt, booking.getNextActionAt());
    }

    @Test
    void shouldRequireActionForFollowUp() {
        TrialBooking booking = TrialBooking.schedule(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );

        booking.markAttendance(
                TrialAttendanceStatus.ATTENDED,
                UUID.randomUUID(),
                null
        );

        assertThrows(
                BadRequestException.class,
                () -> booking.recordResult(
                        TrialResult.FOLLOW_UP,
                        null,
                        null,
                        null,
                        null
                )
        );
    }

}
