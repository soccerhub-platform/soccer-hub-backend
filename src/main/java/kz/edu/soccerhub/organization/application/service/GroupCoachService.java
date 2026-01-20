package kz.edu.soccerhub.organization.application.service;

import kz.edu.soccerhub.common.dto.group.GroupCoachDto;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.GroupCoachPort;
import kz.edu.soccerhub.organization.application.mapper.GroupCoachMapper;
import kz.edu.soccerhub.organization.domain.model.GroupCoach;
import kz.edu.soccerhub.organization.domain.model.enums.CoachRole;
import kz.edu.soccerhub.organization.domain.repository.GroupCoachRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class GroupCoachService implements GroupCoachPort {

    private final GroupCoachRepository groupCoachRepository;

    @Override
    @Transactional
    public UUID assignCoach(UUID groupId, UUID coachId, CoachRole role) {
        if (groupCoachRepository.existsByGroupIdAndCoachIdAndActiveTrue(groupId, coachId)) {
            throw new BadRequestException(
                    "Coach already assigned to this group", Map.of("groupId", groupId, "coachId", coachId));
        }

        role = role == null ? CoachRole.ASSISTANT : role;

        if (role == CoachRole.MAIN && groupCoachRepository.existsByGroupIdAndCoachIdAndRole(groupId, coachId, CoachRole.MAIN)) {
            throw new BadRequestException("Main coach already exist in this group", groupId);
        }

        // 2. Проверяем, был ли раньше
        GroupCoach groupCoach = groupCoachRepository
                .findByGroupIdAndCoachId(groupId, coachId)
                .orElseGet(() -> GroupCoach.builder()
                        .id(UUID.randomUUID())
                        .groupId(groupId)
                        .coachId(coachId)
                        .build()
                );

        groupCoach.setRole(role);
        groupCoach.setActive(true);
        groupCoach.setAssignedFrom(LocalDate.now());
        groupCoach.setAssignedTo(null);

        GroupCoach saved = groupCoachRepository.save(groupCoach);

        log.info("Coach {} assigned to group {}", coachId, groupId);

        return saved.getId();
    }

    @Override
    @Transactional
    public boolean unassignCoach(UUID groupCoachId) {

        GroupCoach groupCoach = groupCoachRepository
                .findById(groupCoachId)
                .orElseThrow(() ->
                        new NotFoundException("Group coach data not found", groupCoachId)
                );

        if (!groupCoach.isActive()) {
            return false;
        }

        groupCoach.setActive(false);
        groupCoach.setAssignedTo(LocalDate.now());

        groupCoachRepository.save(groupCoach);

        log.info("Coach {} unassigned from group {}", groupCoach.getCoachId(), groupCoach.getGroupId());

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GroupCoachDto> getActiveCoaches(UUID groupId) {
        return groupCoachRepository.findByGroupIdAndActiveTrue(groupId)
                .stream()
                .map(GroupCoachMapper::toDto)
                .collect(Collectors.toList());

    }

    @Transactional(readOnly = true)
    public int coachCount(UUID groupId) {
        return groupCoachRepository.countByGroupIdAndActiveTrue(groupId);
    }

}
