package kz.edu.soccerhub.dispatcher.application.dto.coach;
import kz.edu.soccerhub.common.dto.coach.CoachDto;
import lombok.Builder;
import java.util.Set;
import java.util.UUID;
@Builder
public record DispatcherCoachOutput(CoachDto profile,Set<UUID> branchIds,boolean manageable) {}
