package kz.edu.soccerhub.organization.application.service;

import kz.edu.soccerhub.organization.domain.model.GroupActivity;
import kz.edu.soccerhub.organization.domain.model.enums.GroupActivityType;
import kz.edu.soccerhub.organization.domain.repository.GroupActivityRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GroupActivityServiceTest {

    @Test
    void shouldCountSubstitutionsInBothDirections() {
        GroupActivityRepository repository = mock(GroupActivityRepository.class);
        GroupActivityService service = new GroupActivityService(repository);
        UUID coachId = UUID.randomUUID();
        UUID otherCoachId = UUID.randomUUID();
        LocalDateTime from = LocalDateTime.of(2026, 7, 13, 0, 0);
        LocalDateTime to = from.plusWeeks(1);

        when(repository.findByActivityTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThan(
                GroupActivityType.SESSION_COACH_SUBSTITUTED,
                from,
                to
        )).thenReturn(List.of(
                activity(Map.of("substituteCoachId", coachId)),
                activity(Map.of("replacedCoachId", coachId)),
                activity(Map.of("substituteCoachId", otherCoachId)),
                activity(Map.of())
        ));

        assertEquals(2, service.countCoachSubstitutions(coachId, from, to));
    }

    private GroupActivity activity(Map<String, Object> payload) {
        return GroupActivity.builder()
                .id(UUID.randomUUID())
                .groupId(UUID.randomUUID())
                .activityType(GroupActivityType.SESSION_COACH_SUBSTITUTED)
                .payload(payload)
                .build();
    }
}
