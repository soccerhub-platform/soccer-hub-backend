package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.group.GroupActivityDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface GroupActivityPort {

    Page<GroupActivityDto> getGroupActivity(UUID groupId, Pageable pageable);

    void recordGroupActivity(UUID groupId, UUID actorUserId, String activityType, Map<String, Object> payload);

    void recordGroupActivity(
            UUID groupId,
            UUID actorUserId,
            String activityType,
            Map<String, Object> payload,
            UUID correlationId
    );
}
