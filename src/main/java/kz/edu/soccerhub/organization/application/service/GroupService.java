package kz.edu.soccerhub.organization.application.service;

import kz.edu.soccerhub.common.dto.group.CreateGroupCommand;
import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.organization.application.mapper.GroupMapper;
import kz.edu.soccerhub.organization.domain.model.Group;
import kz.edu.soccerhub.organization.domain.model.enums.GroupStatus;
import kz.edu.soccerhub.organization.domain.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService implements GroupPort {

    private final GroupRepository groupRepository;

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

    private void validateAgeRange(Integer from, Integer to) {
        if (from != null && to != null && from > to) {
            throw new BadRequestException("Invalid age range: 'from' age cannot be greater then 'to' age.", from, to);
        }
    }

}
