package kz.edu.soccerhub.organization.domain.repository;

import jakarta.persistence.EntityManager;
import kz.edu.soccerhub.organization.domain.model.GroupMembership;
import kz.edu.soccerhub.organization.domain.model.enums.GroupMembershipStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false"
})
class GroupMembershipRepositoryTest {

    @Autowired
    private GroupMembershipRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldReconcileOnlyDateDrivenStatuses() {
        LocalDate today = LocalDate.of(2026, 7, 16);
        GroupMembership started = membership(GroupMembershipStatus.UPCOMING, today, null);
        GroupMembership future = membership(GroupMembershipStatus.UPCOMING, today.plusDays(1), null);
        GroupMembership expired = membership(GroupMembershipStatus.ACTIVE, today.minusMonths(1), today.minusDays(1));
        GroupMembership onLastDay = membership(GroupMembershipStatus.ACTIVE, today.minusMonths(1), today);
        GroupMembership removed = membership(GroupMembershipStatus.REMOVED, today.minusMonths(1), today.minusDays(1));
        GroupMembership transferred = membership(GroupMembershipStatus.TRANSFERRED, today.minusMonths(1), today.minusDays(1));
        repository.saveAllAndFlush(List.of(started, future, expired, onLastDay, removed, transferred));

        LocalDateTime updatedAt = LocalDateTime.of(2026, 7, 16, 0, 5);
        int completed = repository.completeExpiredMemberships(today, updatedAt, "system:test");
        int activated = repository.activateStartedMemberships(today, updatedAt, "system:test");
        entityManager.clear();

        assertEquals(1, completed);
        assertEquals(1, activated);
        assertStatus(started.getId(), GroupMembershipStatus.ACTIVE);
        assertStatus(future.getId(), GroupMembershipStatus.UPCOMING);
        assertStatus(expired.getId(), GroupMembershipStatus.COMPLETED);
        assertStatus(onLastDay.getId(), GroupMembershipStatus.ACTIVE);
        assertStatus(removed.getId(), GroupMembershipStatus.REMOVED);
        assertStatus(transferred.getId(), GroupMembershipStatus.TRANSFERRED);
    }

    private GroupMembership membership(
            GroupMembershipStatus status,
            LocalDate joinedAt,
            LocalDate leftAt
    ) {
        return GroupMembership.builder()
                .id(UUID.randomUUID())
                .groupId(UUID.randomUUID())
                .playerId(UUID.randomUUID())
                .status(status)
                .joinedAt(joinedAt)
                .leftAt(leftAt)
                .build();
    }

    private void assertStatus(UUID membershipId, GroupMembershipStatus expected) {
        assertEquals(expected, repository.findById(membershipId).orElseThrow().getStatus());
    }
}
