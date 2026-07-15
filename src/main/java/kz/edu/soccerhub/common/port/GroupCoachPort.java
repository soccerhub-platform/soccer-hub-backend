package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.group.GroupCoachDto;
import kz.edu.soccerhub.organization.domain.model.enums.CoachRole;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface GroupCoachPort {

    UUID assignCoach(UUID groupId, UUID coachId, CoachRole role);

    UUID assignCoach(UUID groupId, UUID coachId, CoachRole role, LocalDate assignedFrom, LocalDate assignedTo);

    boolean unassignCoach(UUID groupCoachId);

    Optional<GroupCoachDto> findAssignmentById(UUID groupCoachId);

    Collection<GroupCoachDto> getActiveCoaches(UUID groupId);

    Collection<GroupCoachDto> getActiveAssignmentsByCoachId(UUID coachId);

    Collection<GroupCoachDto> getActiveAssignmentsByCoachIdsAndGroupIds(Set<UUID> coachIds, Set<UUID> groupIds);
}
