package kz.edu.soccerhub.common.port;

import java.util.List;
import java.util.UUID;

public interface GroupCoachPort {

    UUID assignCoach(UUID groupId, UUID coachId);

    boolean unassignCoach(UUID groupId, UUID coachId);

    List<UUID> getActiveCoaches(UUID groupId);
}