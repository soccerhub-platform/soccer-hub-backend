package kz.edu.soccerhub.admin.application.dto.session;

import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionAttendanceStatus;
import kz.edu.soccerhub.common.dto.media.MediaAssetResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminSessionAttendanceOutput(
        UUID sessionId,
        GroupRef group,
        LocalDate sessionDate,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String status,
        String effectiveStatus,
        Summary summary,
        List<ParticipantItem> participants,
        Capabilities capabilities
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

    public record Summary(
            int total,
            int marked,
            int present,
            int absent,
            int excused,
            int late,
            int unmarked,
            int presentLike
    ) {}

    public record ParticipantItem(
            UUID playerId,
            String fullName,
            TrainingSessionAttendanceStatus status,
            String comment
    ) {}

    public record Capabilities(
            boolean canEdit
    ) {}
}
