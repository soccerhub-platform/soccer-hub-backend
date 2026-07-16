package kz.edu.soccerhub.admin.application.dto.session;

import kz.edu.soccerhub.common.dto.media.MediaAssetResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminGroupSessionsOutput(
        UUID groupId,
        LocalDate from,
        LocalDate to,
        List<SessionItem> items
) {
    public record SessionItem(
            UUID id,
            UUID scheduleId,
            LocalDate sessionDate,
            LocalDateTime startsAt,
            LocalDateTime endsAt,
            String status,
            String effectiveStatus,
            String cancelReason,
            LocationRef location,
            List<CoachRef> coaches,
            int participantsCount,
            AttendanceSummary attendance,
            Capabilities capabilities
    ) {}

    public record LocationRef(
            UUID id,
            String name
    ) {}

    public record CoachRef(
            UUID id,
            String fullName,
            String role,
            MediaAssetResponse avatar
    ) {
        public CoachRef(UUID id, String fullName, String role) {
            this(id, fullName, role, null);
        }
    }

    public record AttendanceSummary(
            int total,
            int marked,
            int presentLike
    ) {}

    public record Capabilities(
            boolean canCancel,
            boolean canReschedule,
            boolean canSubstituteCoach,
            boolean canOpenAttendance
    ) {}
}
