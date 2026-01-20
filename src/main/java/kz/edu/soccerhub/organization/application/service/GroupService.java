package kz.edu.soccerhub.organization.application.service;

import kz.edu.soccerhub.common.dto.group.CreateGroupCommand;
import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.organization.application.dto.GroupSummary;
import kz.edu.soccerhub.organization.application.mapper.GroupMapper;
import kz.edu.soccerhub.organization.domain.model.Group;
import kz.edu.soccerhub.organization.domain.model.enums.GroupStatus;
import kz.edu.soccerhub.organization.domain.model.enums.ScheduleStatus;
import kz.edu.soccerhub.organization.domain.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService implements GroupPort {

    private final GroupRepository groupRepository;
    private final GroupCoachService groupCoachService;
    private final GroupScheduleService groupScheduleService;

    @Transactional
    public UUID createGroup(CreateGroupCommand command) {
        final UUID groupId = UUID.randomUUID();

        validateAgeRange(command.ageFrom(), command.ageTo());

        Group group = Group.builder()
                .id(groupId)
                .name(command.name())
                .description(command.description())
                .ageFrom(command.ageFrom())
                .ageTo(command.ageTo())
                .capacity(command.capacity())
                .level(command.level())
                .branchId(command.branchId())
                .status(GroupStatus.ACTIVE)
                .build();

        groupRepository.saveAndFlush(group);
        return groupId;
    }

    @Override
    @Transactional(readOnly = true)
    public GroupDto getGroupById(UUID groupId) {
        return groupRepository.findById(groupId)
                .map(GroupMapper::toDto)
                .orElseThrow(() -> new NotFoundException("Group not found", Map.of("groupId", groupId)));
    }

    @Override
    @Transactional
    public Collection<GroupDto> getGroupsByBranch(UUID branchId) {
        return groupRepository.findByBranchId(branchId).stream()
                .map(GroupMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void updateStatus(UUID groupId, GroupStatus status) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new NotFoundException("Group not found", Map.of("groupId", groupId)));

        group.setStatus(status);
        groupRepository.saveAndFlush(group);
    }

    @Transactional
    public void deleteGroup(UUID groupId) {
        groupRepository.deleteById(groupId);
    }

    @Transactional
    public void stopGroup(UUID groupId) {
        groupRepository.findById(groupId)
                .ifPresent(group -> {
                    group.setStatus(GroupStatus.STOPPED);
                    groupRepository.saveAndFlush(group);
                });
    }

    @Transactional
    public void pauseGroup(UUID groupId) {
        groupRepository.findById(groupId)
                .ifPresent(group -> {
                    group.setStatus(GroupStatus.PAUSED);
                    groupRepository.saveAndFlush(group);
                });
    }

    @Transactional(readOnly = true)
    public GroupSummary summary(UUID groupId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() ->
                        new NotFoundException("Group not found", Map.of("groupId", groupId))
                );

        int coachesCount = groupCoachService.coachCount(groupId);
        int sessionsPerWeek = groupScheduleService.countSessionsPerWeek(groupId);
        boolean scheduleActive = groupScheduleService.existsScheduleByStatus(groupId, ScheduleStatus.ACTIVE);
        LocalDateTime nextSession = groupScheduleService.getNextSession(groupId);
        int studentsCount = 0; // TODO

        return GroupSummary.builder()
                .groupId(groupId)
                .coachesCount(coachesCount)
                .sessionPerWeek(sessionsPerWeek)
                .nextSession(nextSession)
                .studentsCount(studentsCount)
                .capacity(Optional.ofNullable(group.getCapacity()).orElse(0))
                .scheduleActive(scheduleActive)
                .build();
    }

    private void validateAgeRange(Integer from, Integer to) {
        if (from != null && to != null && from > to) {
            throw new BadRequestException("Invalid age range: 'from' age cannot be greater then 'to' age.", from, to);
        }
    }

}
