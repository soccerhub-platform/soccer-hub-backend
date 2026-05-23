package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.group.GroupCoachDto;
import kz.edu.soccerhub.organization.domain.model.enums.CoachRole;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface GroupCoachPort {

    UUID assignCoach(UUID groupId, UUID coachId, CoachRole role);

    boolean unassignCoach(UUID groupCoachId);

    Collection<GroupCoachDto> getActiveCoaches(UUID groupId);

    Collection<GroupCoachDto> getActiveAssignmentsByCoachId(UUID coachId);

    Collection<GroupCoachDto> getActiveAssignmentsByCoachIdsAndGroupIds(Set<UUID> coachIds, Set<UUID> groupIds);
}
