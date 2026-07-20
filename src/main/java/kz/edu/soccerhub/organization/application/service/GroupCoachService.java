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
        return assignCoach(groupId, coachId, role, LocalDate.now(), null);
    }

    @Override
    @Transactional
    public UUID assignCoach(UUID groupId, UUID coachId, CoachRole role, LocalDate assignedFrom, LocalDate assignedTo) {
        if (groupCoachRepository.existsByGroupIdAndCoachIdAndActiveTrue(groupId, coachId)) {
            throw new BadRequestException(
                    "Coach already assigned to this group", Map.of("groupId", groupId, "coachId", coachId));
        }

        role = role == null ? CoachRole.ASSISTANT : role;
        LocalDate resolvedAssignedFrom = assignedFrom == null ? LocalDate.now() : assignedFrom;
        if (assignedTo != null && assignedTo.isBefore(resolvedAssignedFrom)) {
            throw new BadRequestException("assignedTo must not be before assignedFrom", Map.of(
                    "assignedFrom", resolvedAssignedFrom,
                    "assignedTo", assignedTo
            ));
        }

        if (role == CoachRole.MAIN && groupCoachRepository.existsByGroupIdAndRoleAndActiveTrue(groupId, CoachRole.MAIN)) {
            throw new BadRequestException("Main coach already exist in this group", groupId);
        }

        GroupCoach groupCoach = GroupCoach.builder()
                .id(UUID.randomUUID())
                .groupId(groupId)
                .coachId(coachId)
                .role(role)
                .active(true)
                .assignedFrom(resolvedAssignedFrom)
                .assignedTo(assignedTo)
                .build();

        GroupCoach saved = groupCoachRepository.save(groupCoach);

        log.info("Coach {} assigned to group {}", coachId, groupId);

        return saved.getId();
    }

    @Override
    @Transactional
    public boolean unassignCoach(UUID groupCoachId) {
        return unassignCoach(groupCoachId, LocalDate.now(), null, null);
    }

    @Override
    @Transactional
    public boolean unassignCoach(UUID groupCoachId, LocalDate assignedTo, String reason, UUID replacementCoachId) {

        GroupCoach groupCoach = groupCoachRepository
                .findById(groupCoachId)
                .orElseThrow(() ->
                        new NotFoundException("Group coach data not found", groupCoachId)
                );

        if (!groupCoach.isActive()) {
            return false;
        }

        groupCoach.setActive(false);
        groupCoach.setAssignedTo(assignedTo == null ? LocalDate.now() : assignedTo);
        groupCoach.setRemovalReason(reason);
        groupCoach.setReplacementCoachId(replacementCoachId);

        groupCoachRepository.save(groupCoach);

        log.info("Coach {} unassigned from group {}", groupCoach.getCoachId(), groupCoach.getGroupId());

        return true;
    }

    @Override
    @Transactional
    public GroupCoachDto updateRole(UUID groupCoachId, CoachRole role) {
        if (role == null) {
            throw new BadRequestException("Coach role is required", groupCoachId);
        }

        GroupCoach groupCoach = groupCoachRepository.findById(groupCoachId)
                .orElseThrow(() -> new NotFoundException("Group coach data not found", groupCoachId));
        if (!groupCoach.isActive()) {
            throw new BadRequestException("Inactive coach assignment cannot be changed", groupCoachId);
        }
        if (groupCoach.getRole() == role) {
            return GroupCoachMapper.toDto(groupCoach);
        }
        if (role == CoachRole.MAIN
                && groupCoachRepository.existsByGroupIdAndRoleAndActiveTrue(groupCoach.getGroupId(), CoachRole.MAIN)) {
            throw new BadRequestException("Main coach already exist in this group", groupCoach.getGroupId());
        }

        groupCoach.setRole(role);
        return GroupCoachMapper.toDto(groupCoachRepository.save(groupCoach));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GroupCoachDto> findAssignmentById(UUID groupCoachId) {
        return groupCoachRepository.findById(groupCoachId)
                .map(GroupCoachMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GroupCoachDto> getActiveCoaches(UUID groupId) {
        return groupCoachRepository.findByGroupIdAndActiveTrue(groupId)
                .stream()
                .map(GroupCoachMapper::toDto)
                .collect(Collectors.toList());

    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GroupCoachDto> getAssignmentsByGroupId(UUID groupId) {
        return groupCoachRepository.findByGroupIdOrderByAssignedFromDescCreatedAtDesc(groupId)
                .stream()
                .map(GroupCoachMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GroupCoachDto> getAssignmentsByCoachId(UUID coachId) {
        return groupCoachRepository.findByCoachIdOrderByAssignedFromDescCreatedAtDesc(coachId)
                .stream()
                .map(GroupCoachMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GroupCoachDto> getActiveAssignmentsByCoachId(UUID coachId) {
        return groupCoachRepository.findByCoachIdAndActiveTrue(coachId)
                .stream()
                .map(GroupCoachMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<GroupCoachDto> getActiveAssignmentsByCoachIdsAndGroupIds(Set<UUID> coachIds, Set<UUID> groupIds) {
        if (coachIds == null || coachIds.isEmpty() || groupIds == null || groupIds.isEmpty()) {
            return List.of();
        }
        return groupCoachRepository.findByCoachIdInAndGroupIdInAndActiveTrue(coachIds, groupIds)
                .stream()
                .map(GroupCoachMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public int coachCount(UUID groupId) {
        return groupCoachRepository.countByGroupIdAndActiveTrue(groupId);
    }

}
