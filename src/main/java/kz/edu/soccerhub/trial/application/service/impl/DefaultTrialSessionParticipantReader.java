package kz.edu.soccerhub.trial.application.service.impl;

import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;
import kz.edu.soccerhub.common.dto.trial.TrialSessionParticipantDto;
import kz.edu.soccerhub.common.port.TrialLeadPort;
import kz.edu.soccerhub.common.port.TrialStudentDetailsPort;
import kz.edu.soccerhub.trial.application.service.TrialSessionParticipantReader;
import kz.edu.soccerhub.trial.domain.entity.TrialBooking;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DefaultTrialSessionParticipantReader
        implements TrialSessionParticipantReader {

    private final TrialStudentDetailsPort studentPort;
    private final TrialLeadPort leadPort;

    @Override
    public List<TrialSessionParticipantDto> read(
            List<TrialBooking> bookings
    ) {
        if (bookings == null || bookings.isEmpty()) {
            return List.of();
        }

        Set<UUID> studentIds = bookings.stream()
                .map(TrialBooking::getStudentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<UUID> participantIds = bookings.stream()
                .map(TrialBooking::getParticipantId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, TrialBookingDetailsDto.Student> students =
                studentPort.getDetails(studentIds);

        Map<UUID, TrialBookingDetailsDto.Student> participants =
                leadPort.getParticipantDetails(participantIds);

        return bookings.stream()
                .map(booking -> toOutput(
                        booking,
                        students,
                        participants
                ))
                .toList();
    }

    private TrialSessionParticipantDto toOutput(
            TrialBooking booking,
            Map<UUID, TrialBookingDetailsDto.Student> students,
            Map<UUID, TrialBookingDetailsDto.Student> participants
    ) {
        TrialBookingDetailsDto.Student child =
                booking.getStudentId() != null
                        ? students.get(booking.getStudentId())
                        : participants.get(booking.getParticipantId());

        return TrialSessionParticipantDto.builder()
                .trialBookingId(booking.getId())
                .leadId(booking.getLeadId())
                .participantId(booking.getParticipantId())
                .studentId(booking.getStudentId())
                .fullName(child == null ? null : child.fullName())
                .birthDate(child == null ? null : child.birthDate())
                .age(child == null ? null : child.age())
                .bookingStatus(booking.getStatus())
                .attendanceStatus(booking.getAttendanceStatus())
                .attendanceComment(booking.getAttendanceComment())
                .result(booking.getResult())
                .coachFeedback(booking.getCoachFeedback())
                .coachRecommendation(booking.getCoachRecommendation())
                .coachRecommendedGroupId(booking.getCoachRecommendedGroupId())
                .coachRecommendationComment(booking.getCoachRecommendationComment())
                .coachRecommendationAt(booking.getCoachRecommendationAt())
                .coachRecommendationBy(booking.getCoachRecommendationBy())
                .build();
    }
}