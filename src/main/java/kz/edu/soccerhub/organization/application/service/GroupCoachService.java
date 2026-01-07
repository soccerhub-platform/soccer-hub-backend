package kz.edu.soccerhub.organization.application.service;

import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.port.GroupCoachPort;
import kz.edu.soccerhub.organization.domain.model.GroupCoach;
import kz.edu.soccerhub.organization.domain.model.enums.CoachRole;
import kz.edu.soccerhub.organization.domain.repository.GroupCoachRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class GroupCoachService implements GroupCoachPort {

    private final GroupCoachRepository groupCoachRepository;

    @Override
    @Transactional
    public UUID assignCoach(UUID groupId, UUID coachId) {
        if (groupCoachRepository.existsByGroupIdAndCoachIdAndActiveTrue(groupId, coachId)) {
            throw new IllegalStateException(
                    "Coach already assigned to this group"
            );
        }

        // 2. Проверяем, был ли раньше
        GroupCoach groupCoach = groupCoachRepository
                .findByGroupIdAndCoachId(groupId, coachId)
                .orElseGet(() -> GroupCoach.builder()
                        .id(UUID.randomUUID())
                        .groupId(groupId)
                        .coachId(coachId)
                        .role(CoachRole.MAIN)
                        .build()
                );

        groupCoach.setActive(true);
        groupCoach.setAssignedFrom(LocalDate.now());
        groupCoach.setAssignedTo(null);

        GroupCoach saved = groupCoachRepository.save(groupCoach);

        log.info("Coach {} assigned to group {}", coachId, groupId);

        return saved.getId();
    }

    @Override
    @Transactional
    public boolean unassignCoach(UUID groupId, UUID coachId) {

        GroupCoach groupCoach = groupCoachRepository
                .findByGroupIdAndCoachId(groupId, coachId)
                .orElseThrow(() ->
                        new IllegalStateException("Coach is not assigned to this group")
                );

        if (!groupCoach.isActive()) {
            return false;
        }

        groupCoach.setActive(false);
        groupCoach.setAssignedTo(LocalDate.now());

        groupCoachRepository.save(groupCoach);

        log.info("Coach {} unassigned from group {}", coachId, groupId);

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> getActiveCoaches(UUID groupId) {
        return groupCoachRepository.findByGroupIdAndActiveTrue(groupId)
                .stream()
                .map(GroupCoach::getCoachId)
                .toList();
    }

    private void validateCoachAssignedBeforeScheduling(
            UUID groupId,
            UUID coachId
    ) {
        boolean assigned = groupCoachRepository
                .existsByGroupIdAndCoachIdAndActiveTrue(groupId, coachId);

        if (!assigned) {
            throw new BadRequestException(
                    "Coach is not assigned to group",
                    Map.of(
                            "groupId", groupId,
                            "coachId", coachId
                    )
            );
        }
    }

}
