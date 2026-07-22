package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.client.ClientActivityDto;
import kz.edu.soccerhub.common.dto.client.ClientActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface ClientActivityPort {

    Page<ClientActivityDto> getClientActivity(UUID clientId, Pageable pageable);

    void recordClientActivity(UUID clientId, UUID actorUserId, ClientActivityType activityType, Map<String, Object> payload);
}
