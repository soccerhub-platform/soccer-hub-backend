package kz.edu.soccerhub.admin.application.dto.session;

import kz.edu.soccerhub.common.dto.media.MediaAssetResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminSessionDetailsOutput(
        UUID id,
        GroupRef group,
        UUID scheduleId,
        LocalDate sessionDate,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        LocalDateTime actualStartAt,
        LocalDateTime actualEndAt,
        String status,
        String effectiveStatus,
        String cancelReason,
        AdminGroupSessionsOutput.LocationRef location,
        List<AdminGroupSessionsOutput.CoachRef> coaches,
        int participantsCount,
        AdminGroupSessionsOutput.AttendanceSummary attendance,
        AdminGroupSessionsOutput.Capabilities capabilities
) {
    public record GroupRef(
            UUID id,
            String name,
            MediaAssetResponse avatar
    ) {
        public GroupRef(UUID id, String name) {
            this(id, name, null);
        }
    }
}
