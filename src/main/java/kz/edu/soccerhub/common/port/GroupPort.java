package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.group.CreateGroupCommand;
import kz.edu.soccerhub.common.dto.group.GroupDto;

import java.util.Collection;
import java.util.UUID;

public interface GroupPort {
    UUID createGroup(CreateGroupCommand command);
    GroupDto getGroupById(UUID groupId);
    void deleteGroup(UUID groupId);
    void stopGroup(UUID groupId);
    void pauseGroup(UUID groupId);
    Collection<GroupDto> getGroupsByBranch(UUID branchId);
}
