package kz.edu.soccerhub.client.application;

import kz.edu.soccerhub.client.domain.model.ClientActivity;
import kz.edu.soccerhub.client.domain.repository.ClientActivityRepository;
import kz.edu.soccerhub.common.dto.client.ClientActivityDto;
import kz.edu.soccerhub.common.dto.client.ClientActivityType;
import kz.edu.soccerhub.common.port.ClientActivityPort;
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
public class ClientActivityService implements ClientActivityPort {

    private final ClientActivityRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Page<ClientActivityDto> getClientActivity(UUID clientId, Pageable pageable) {
        return repository.findByClientIdOrderByOccurredAtDesc(clientId, pageable)
                .map(activity -> new ClientActivityDto(activity.getId(), activity.getActivityType().name(),
                        activity.getOccurredAt(), activity.getActorUserId(), activity.getPayload()));
    }

    @Override
    @Transactional
    public void recordClientActivity(UUID clientId, UUID actorUserId, ClientActivityType activityType, Map<String, Object> payload) {
        repository.save(ClientActivity.builder()
                .id(UUID.randomUUID())
                .clientId(clientId)
                .actorUserId(actorUserId)
                .activityType(activityType)
                .payload(payload == null ? Map.of() : new LinkedHashMap<>(payload))
                .build());
    }
}
