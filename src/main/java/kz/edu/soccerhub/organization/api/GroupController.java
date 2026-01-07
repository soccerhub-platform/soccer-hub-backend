package kz.edu.soccerhub.organization.api;

import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.organization.application.service.GroupCoachService;
import kz.edu.soccerhub.organization.application.service.GroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/organization/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupCoachService groupCoachQueryService;
    private final GroupService groupService;

    @GetMapping("/{groupId}")
    public GroupDto getGroupById(@PathVariable UUID groupId) {
        return groupService.getGroupById(groupId);
    }

    @GetMapping("/branches/{branchId}")
    public Collection<GroupDto> getAllBranchGroups(@PathVariable UUID branchId) {
        return groupService.getGroupsByBranch(branchId);
    }

    @GetMapping("/{groupId}/coaches")
    public List<UUID> getGroupCoaches(@PathVariable UUID groupId) {
        return groupCoachQueryService.getActiveCoaches(groupId);
    }
}