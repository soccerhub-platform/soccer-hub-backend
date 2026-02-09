package kz.edu.soccerhub.common.port;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.common.dto.coach.CoachCreateCommand;
import kz.edu.soccerhub.common.dto.coach.CoachDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface CoachPort {
    UUID create(@Valid CoachCreateCommand command);
    void assignToBranch(@NotNull UUID coachId, @NotNull UUID branchId);
    void unassignFromBranch(@NotNull UUID coachId, @NotNull UUID branchId);
    Page<CoachDto> getCoaches(Set<UUID> branchId, Pageable pageable);
    CoachDto getCoach(UUID coachId);
    Collection<CoachDto> getCoaches(Set<UUID> coachIds);
    boolean verifyCoach(UUID coachId);
    void enableCoach(UUID coachId);
    void disableCoach(UUID coachId);
}
