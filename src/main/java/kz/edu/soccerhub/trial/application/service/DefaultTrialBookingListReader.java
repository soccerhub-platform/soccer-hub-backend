package kz.edu.soccerhub.trial.application.service;

import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;
import kz.edu.soccerhub.common.dto.trial.TrialBookingListItemDto;
import kz.edu.soccerhub.common.dto.trial.TrialSessionContext;
import kz.edu.soccerhub.common.port.TrialCoachPort;
import kz.edu.soccerhub.common.port.TrialGroupPort;
import kz.edu.soccerhub.common.port.TrialLeadPort;
import kz.edu.soccerhub.common.port.TrialLocationPort;
import kz.edu.soccerhub.common.port.TrialSessionPort;
import kz.edu.soccerhub.common.port.TrialStudentDetailsPort;
import kz.edu.soccerhub.trial.domain.entity.TrialBooking;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DefaultTrialBookingListReader
        implements TrialBookingListReader {

    private final TrialSessionPort sessionPort;
    private final TrialStudentDetailsPort studentPort;
    private final TrialLeadPort leadPort;
    private final TrialGroupPort groupPort;
    private final TrialCoachPort coachPort;
    private final TrialLocationPort locationPort;

    @Override
    public Page<TrialBookingListItemDto> read(Page<TrialBooking> bookings) {
        List<TrialBooking> content = bookings.getContent();

        Set<UUID> sessionIds = content.stream()
                .map(TrialBooking::getTrainingSessionId)
                .collect(Collectors.toSet());

        Set<UUID> studentIds = content.stream()
                .map(TrialBooking::getStudentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<UUID> participantIds = content.stream()
                .map(TrialBooking::getParticipantId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<UUID> leadIds = content.stream()
                .map(TrialBooking::getLeadId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, TrialSessionContext> sessions =
                sessionPort.getSessionDetails(sessionIds);

        Map<UUID, TrialBookingDetailsDto.Student> students =
                studentPort.getDetails(studentIds);

        Map<UUID, TrialBookingDetailsDto.Student> participants =
                leadPort.getParticipantDetails(participantIds);

        Map<UUID, TrialBookingDetailsDto.Lead> leads =
                leadPort.getDetails(leadIds);

        Set<UUID> groupIds = sessions.values().stream()
                .map(TrialSessionContext::groupId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<UUID> coachIds = sessions.values().stream()
                .map(TrialSessionContext::coachId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<UUID> locationIds = sessions.values().stream()
                .map(TrialSessionContext::locationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, TrialBookingDetailsDto.Group> groups =
                groupPort.getDetails(groupIds);

        Map<UUID, TrialBookingDetailsDto.Coach> coaches =
                coachPort.getDetails(coachIds);

        Map<UUID, TrialBookingDetailsDto.Location> locations =
                locationPort.getDetails(locationIds);

        return bookings.map(booking -> {
            TrialSessionContext session = getOrNull(sessions, booking.getTrainingSessionId());

            TrialBookingDetailsDto.Student student =
                    booking.getStudentId() != null
                            ? getOrNull(students, booking.getStudentId())
                            : getOrNull(participants, booking.getParticipantId());

            TrialBookingDetailsDto.Lead lead =
                    getOrNull(leads, booking.getLeadId());

            TrialBookingDetailsDto.Group group =
                    session == null
                            ? null
                            : getOrNull(groups, session.groupId());

            TrialBookingDetailsDto.Coach coach =
                    session == null
                            ? null
                            : getOrNull(coaches, session.coachId());

            TrialBookingDetailsDto.Location location =
                    session == null
                            ? null
                            : getOrNull(locations, session.locationId());

            return TrialBookingListItemDto.builder()
                    .id(booking.getId())
                    .leadId(booking.getLeadId())
                    .clientId(booking.getClientId())
                    .studentId(booking.getStudentId())
                    .trainingSessionId(booking.getTrainingSessionId())
                    .studentName(student == null ? null : student.fullName())
                    .leadName(lead == null ? null : lead.fullName())
                    .leadPhone(lead == null ? null : lead.phone())
                    .leadEmail(lead == null ? null : lead.email())
                    .sessionDate(session == null
                            ? null
                            : session.startsAt().toLocalDate())
                    .sessionStartsAt(session == null
                            ? null
                            : session.startsAt())
                    .sessionEndsAt(session == null
                            ? null
                            : session.endsAt())
                    .groupName(group == null ? null : group.name())
                    .coachName(coach == null ? null : coach.fullName())
                    .locationName(location == null ? null : location.name())
                    .status(booking.getStatus())
                    .attendanceStatus(booking.getAttendanceStatus())
                    .result(booking.getResult())
                    .nextActionType(booking.getNextActionType())
                    .nextActionAt(booking.getNextActionAt())
                    .build();
        });
    }

    private static <K, V> V getOrNull(Map<K, V> map, K key) {
        return key == null ? null : map.get(key);
    }
}