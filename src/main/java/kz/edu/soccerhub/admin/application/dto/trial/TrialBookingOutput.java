package kz.edu.soccerhub.admin.application.dto.trial;

import kz.edu.soccerhub.common.dto.trial.TrialBookingDto;
import kz.edu.soccerhub.trial.domain.enums.TrialAttendanceStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialBookingStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialResult;
import lombok.Builder;

import java.util.UUID;

@Builder
public record TrialBookingOutput(
        UUID id,
        UUID leadId,
        UUID clientId,
        UUID studentId,
        UUID trainingSessionId,
        TrialBookingStatus status,
        TrialAttendanceStatus attendanceStatus,
        TrialResult result
) {
    public static TrialBookingOutput from(TrialBookingDto booking) {
        return new TrialBookingOutput(
                booking.id(),
                booking.leadId(),
                booking.clientId(),
                booking.studentId(),
                booking.trainingSessionId(),
                booking.status(),
                booking.attendanceStatus(),
                booking.result()
        );
    }
}
