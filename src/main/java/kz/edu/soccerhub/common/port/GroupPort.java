package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.group.CreateGroupCommand;
import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.organization.domain.model.enums.GroupStatus;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface GroupPort {
    UUID createGroup(CreateGroupCommand command);
    GroupDto getGroupById(UUID groupId);
    Collection<GroupDto> getGroupsByIds(Set<UUID> groupIds);
    void deleteGroup(UUID groupId);
    void stopGroup(UUID groupId);
    void pauseGroup(UUID groupId);
    Collection<GroupDto> getGroupsByBranch(UUID branchId);
    void updateStatus(UUID groupId, GroupStatus status);
}
