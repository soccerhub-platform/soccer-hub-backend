package kz.edu.soccerhub.organization.application.service;

import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.organization.domain.model.GroupCoach;
import kz.edu.soccerhub.organization.domain.model.enums.CoachRole;
import kz.edu.soccerhub.organization.domain.repository.GroupCoachRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupCoachServiceTest {

    @Mock
    private GroupCoachRepository repository;

    @Test
    void shouldRejectSecondActiveMainCoach() {
        UUID groupId = UUID.randomUUID();
        UUID coachId = UUID.randomUUID();
        GroupCoachService service = new GroupCoachService(repository);

        when(repository.existsByGroupIdAndCoachIdAndActiveTrue(groupId, coachId)).thenReturn(false);
        when(repository.existsByGroupIdAndRoleAndActiveTrue(groupId, CoachRole.MAIN)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.assignCoach(
                groupId,
                coachId,
                CoachRole.MAIN,
                LocalDate.now(),
                null
        ));
    }

    @Test
    void shouldCreateNewAssignmentAndKeepHistoricalRecordUntouched() {
        UUID historicalAssignmentId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID coachId = UUID.randomUUID();
        LocalDate assignedFrom = LocalDate.now().plusDays(1);
        GroupCoach existing = GroupCoach.builder()
                .id(historicalAssignmentId)
                .groupId(groupId)
                .coachId(coachId)
                .role(CoachRole.MAIN)
                .active(false)
                .assignedTo(LocalDate.now())
                .build();
        GroupCoachService service = new GroupCoachService(repository);

        when(repository.existsByGroupIdAndCoachIdAndActiveTrue(groupId, coachId)).thenReturn(false);
        when(repository.existsByGroupIdAndRoleAndActiveTrue(groupId, CoachRole.MAIN)).thenReturn(false);
        when(repository.save(org.mockito.ArgumentMatchers.any(GroupCoach.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UUID result = service.assignCoach(groupId, coachId, CoachRole.MAIN, assignedFrom, null);

        assertTrue(result != null);
        assertTrue(!existing.isActive());
        assertEquals(LocalDate.now(), existing.getAssignedTo());
    }

    @Test
    void shouldRejectPromotionWhenGroupAlreadyHasMainCoach() {
        UUID assignmentId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        GroupCoach assignment = GroupCoach.builder()
                .id(assignmentId)
                .groupId(groupId)
                .coachId(UUID.randomUUID())
                .role(CoachRole.ASSISTANT)
                .active(true)
                .build();
        GroupCoachService service = new GroupCoachService(repository);

        when(repository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(repository.existsByGroupIdAndRoleAndActiveTrue(groupId, CoachRole.MAIN)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.updateRole(assignmentId, CoachRole.MAIN));
    }

    @Test
    void shouldChangeActiveCoachRole() {
        UUID assignmentId = UUID.randomUUID();
        GroupCoach assignment = GroupCoach.builder()
                .id(assignmentId)
                .groupId(UUID.randomUUID())
                .coachId(UUID.randomUUID())
                .role(CoachRole.MAIN)
                .active(true)
                .build();
        GroupCoachService service = new GroupCoachService(repository);

        when(repository.findById(assignmentId)).thenReturn(Optional.of(assignment));
        when(repository.save(assignment)).thenReturn(assignment);

        var updated = service.updateRole(assignmentId, CoachRole.ASSISTANT);

        assertEquals(CoachRole.ASSISTANT, updated.role());
        verify(repository).save(assignment);
    }
}
