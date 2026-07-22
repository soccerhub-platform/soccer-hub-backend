package kz.edu.soccerhub.client.application;

import kz.edu.soccerhub.client.domain.model.ClientActivity;
import kz.edu.soccerhub.common.dto.client.ClientActivityType;
import kz.edu.soccerhub.client.domain.repository.ClientActivityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientActivityServiceTest {

    @Mock private ClientActivityRepository repository;

    @Test
    void shouldRecordAndReadActivityThroughPortModel() {
        ClientActivityService service = new ClientActivityService(repository);
        UUID clientId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        service.recordClientActivity(clientId, actorId, ClientActivityType.CLIENT_UPDATED, Map.of("field", "phone"));

        ArgumentCaptor<ClientActivity> saved = ArgumentCaptor.forClass(ClientActivity.class);
        verify(repository).save(saved.capture());
        assertEquals(clientId, saved.getValue().getClientId());
        assertEquals("phone", saved.getValue().getPayload().get("field"));

        ClientActivity activity = saved.getValue();
        activity.setOccurredAt(LocalDateTime.now());
        PageRequest pageable = PageRequest.of(0, 20);
        when(repository.findByClientIdOrderByOccurredAtDesc(clientId, pageable))
                .thenReturn(new PageImpl<>(List.of(activity), pageable, 1));

        var output = service.getClientActivity(clientId, pageable).getContent().getFirst();
        assertEquals("CLIENT_UPDATED", output.type());
        assertEquals(actorId, output.actorUserId());
    }
}
