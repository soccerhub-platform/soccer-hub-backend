package kz.edu.soccerhub.common.dto.lead;
import lombok.Builder;
import java.time.Instant;
@Builder
public record LeadWorkOutput(String priority, String nextAction, Instant nextActionAt,
                             Instant lastContactAt, Instant stageChangedAt, long version) {}
