package kz.edu.soccerhub.common.dto.trial;

import kz.edu.soccerhub.trial.domain.enums.TrialAttendanceStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialBookingStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialNextActionType;
import kz.edu.soccerhub.trial.domain.enums.TrialResult;
import kz.edu.soccerhub.trial.domain.entity.TrialBooking;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record TrialBookingDto(
        UUID id,
        UUID leadId,
        UUID clientId,
        UUID participantId,
        UUID studentId,
        UUID trainingSessionId,
        TrialBookingStatus status,
        TrialAttendanceStatus attendanceStatus,
        TrialResult result,
        TrialNextActionType nextActionType,
        LocalDateTime nextActionAt
) {

    public static TrialBookingDto from(
            TrialBooking booking
    ) {
        return TrialBookingDto.builder()
                .id(booking.getId())
                .leadId(booking.getLeadId())
                .clientId(booking.getClientId())
                .participantId(booking.getParticipantId())
                .studentId(booking.getStudentId())
                .trainingSessionId(booking.getTrainingSessionId())
                .status(booking.getStatus())
                .attendanceStatus(booking.getAttendanceStatus())
                .result(booking.getResult())
                .nextActionType(booking.getNextActionType())
                .nextActionAt(booking.getNextActionAt())
                .build();
    }
}
