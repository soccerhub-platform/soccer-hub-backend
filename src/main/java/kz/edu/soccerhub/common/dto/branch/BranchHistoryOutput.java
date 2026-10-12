package kz.edu.soccerhub.common.dto.branch;
import lombok.Builder;
import java.time.Instant;
import java.util.UUID;
@Builder
public record BranchHistoryOutput(UUID id,String event,String changedBy,Instant createdAt,BranchDto before,BranchDto after) {}
