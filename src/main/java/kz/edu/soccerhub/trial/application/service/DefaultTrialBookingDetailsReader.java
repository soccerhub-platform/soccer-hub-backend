package kz.edu.soccerhub.trial.application.service;

import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;
import kz.edu.soccerhub.common.dto.trial.TrialSessionContext;
import kz.edu.soccerhub.common.port.TrialSessionPort;
import kz.edu.soccerhub.common.port.TrialStudentDetailsPort;
import kz.edu.soccerhub.common.port.TrialLeadPort;
import kz.edu.soccerhub.common.port.TrialGroupPort;
import kz.edu.soccerhub.common.port.TrialCoachPort;
import kz.edu.soccerhub.common.port.TrialLocationPort;
import kz.edu.soccerhub.trial.domain.entity.TrialBooking;
import kz.edu.soccerhub.trial.domain.enums.TrialAttendanceStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialBookingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultTrialBookingDetailsReader
        implements TrialBookingDetailsReader {

    private final TrialStudentDetailsPort studentDetailsPort;
    private final TrialSessionPort sessionPort;
    private final TrialLeadPort leadPort;
    private final TrialGroupPort groupPort;
    private final TrialCoachPort coachPort;
    private final TrialLocationPort locationPort;

    @Override
    public TrialBookingDetailsDto read(TrialBooking booking) {
        TrialSessionContext session =
                sessionPort.getSessionDetails(
                        booking.getTrainingSessionId()
                );

        return TrialBookingDetailsDto.builder()
                .id(booking.getId())
                .status(booking.getStatus())
                .attendanceStatus(booking.getAttendanceStatus())
                .result(booking.getResult())
                .student(booking.getStudentId() != null
                        ? studentDetailsPort.getDetails(booking.getStudentId())
                        : leadPort.getParticipantDetails(booking.getLeadId(), booking.getParticipantId()))
                .lead(booking.getLeadId() == null
                        ? null
                        : leadPort.getDetails(booking.getLeadId()))
                .group(session.groupId() == null
                        ? null
                        : groupPort.getDetails(session.groupId()))
                .coach(session.coachId() == null
                        ? null
                        : coachPort.getDetails(session.coachId()))
                .location(session.locationId() == null
                        ? null
                        : locationPort.getDetails(session.locationId()))
                .session(
                        TrialBookingDetailsDto.Session.builder()
                                .id(session.sessionId())
                                .date(session.startsAt().toLocalDate())
                                .startsAt(session.startsAt())
                                .endsAt(session.endsAt())
                                .status(session.status().name())
                                .build()
                )
                .attendance(
                        TrialBookingDetailsDto.Attendance.builder()
                                .status(booking.getAttendanceStatus())
                                .markedAt(booking.getAttendanceMarkedAt())
                                .markedBy(booking.getAttendanceMarkedBy())
                                .comment(booking.getAttendanceComment())
                                .build()
                )
                .outcome(
                        TrialBookingDetailsDto.Outcome.builder()
                                .result(booking.getResult())
                                .coachFeedback(booking.getCoachFeedback())
                                .recommendedGroupId(
                                        booking.getRecommendedGroupId()
                                )
                                .build()
                )
                .nextAction(buildNextAction(booking))
                .capabilities(buildCapabilities(booking))
                .build();
    }

    private TrialBookingDetailsDto.NextAction buildNextAction(
            TrialBooking booking
    ) {
        if (booking.getNextActionType() == null) {
            return null;
        }

        return TrialBookingDetailsDto.NextAction.builder()
                .type(booking.getNextActionType().name())
                .dueAt(booking.getNextActionAt())
                .build();
    }

    private TrialBookingDetailsDto.Capabilities buildCapabilities(
            TrialBooking booking
    ) {
        TrialBookingStatus status = booking.getStatus();

        boolean scheduled =
                status == TrialBookingStatus.SCHEDULED;

        boolean confirmed =
                status == TrialBookingStatus.CONFIRMED;

        boolean completed =
                status == TrialBookingStatus.COMPLETED;

        boolean canceled =
                status == TrialBookingStatus.CANCELED;

        return TrialBookingDetailsDto.Capabilities.builder()
                .canConfirm(scheduled)
                .canCancel(scheduled || confirmed)
                .canReschedule(scheduled || confirmed)
                .canMarkAttendance(!canceled && !completed)
                .canRecordResult(
                        completed
                                && booking.getAttendanceStatus()
                                == TrialAttendanceStatus.ATTENDED
                )
                .build();
    }
}
