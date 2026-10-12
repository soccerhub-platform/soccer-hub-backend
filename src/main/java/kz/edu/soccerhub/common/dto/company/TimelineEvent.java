package kz.edu.soccerhub.common.dto.company;
import lombok.Builder;
import java.time.Instant;
import java.util.UUID;
@Builder
public record TimelineEvent(UUID id,String type,String description,Instant occurredAt,UUID actorId) {}
