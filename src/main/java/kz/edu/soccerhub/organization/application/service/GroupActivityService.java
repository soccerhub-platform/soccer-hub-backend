package kz.edu.soccerhub.organization.application.service;

import kz.edu.soccerhub.common.dto.group.GroupActivityDto;
import kz.edu.soccerhub.common.port.GroupActivityPort;
import kz.edu.soccerhub.organization.domain.model.GroupActivity;
import kz.edu.soccerhub.organization.domain.model.enums.GroupActivityType;
import kz.edu.soccerhub.organization.domain.repository.GroupActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroupActivityService implements GroupActivityPort {

    private final GroupActivityRepository groupActivityRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<GroupActivityDto> getGroupActivity(UUID groupId, Pageable pageable) {
        return groupActivityRepository.findByGroupIdOrderByOccurredAtDesc(groupId, pageable)
                .map(this::toOutput);
    }

    @Transactional
    public void recordGroupActivity(UUID groupId, UUID actorUserId, String activityType, Map<String, Object> payload) {
        recordGroupActivity(groupId, actorUserId, activityType, payload, null);
    }

    @Override
    @Transactional
    public void recordGroupActivity(
            UUID groupId,
            UUID actorUserId,
            String activityType,
            Map<String, Object> payload,
            UUID correlationId
    ) {
        groupActivityRepository.save(GroupActivity.builder()
                .id(UUID.randomUUID())
                .groupId(groupId)
                .actorUserId(actorUserId)
                .activityType(GroupActivityType.valueOf(activityType))
                .payload(payload == null ? Map.of() : new LinkedHashMap<>(payload))
                .correlationId(correlationId)
                .build());
    }

    private GroupActivityDto toOutput(GroupActivity activity) {
        UUID actorUserId = activity.getActorUserId();
        return new GroupActivityDto(
                activity.getId(),
                activity.getActivityType().name(),
                activity.getOccurredAt(),
                actorUserId == null ? null : new GroupActivityDto.ActorRef(actorUserId, null),
                activity.getPayload() == null ? Map.of() : activity.getPayload()
        );
    }
}
