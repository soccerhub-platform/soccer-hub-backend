package kz.edu.soccerhub.dispacher.application.service;

import kz.edu.soccerhub.common.dto.branch.BranchDto;
import kz.edu.soccerhub.common.dto.branch.CreateBranchCommand;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.BranchPort;
import kz.edu.soccerhub.dispacher.application.dto.branch.DispatcherBranchCreateInput;
import kz.edu.soccerhub.dispacher.application.dto.branch.DispatcherBranchCreateOutput;
import kz.edu.soccerhub.dispacher.application.dto.branch.DispatcherBranchesOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatcherBranchService {

    private final DispatcherClubService dispatcherClubService;
    private final BranchPort branchPort;

    @Transactional
    public DispatcherBranchCreateOutput createBranch(UUID dispatcherId, DispatcherBranchCreateInput input) {

        boolean hasAccess = dispatcherClubService.hasAccess(dispatcherId, input.clubId());
        if (!hasAccess) {
            throw new BadRequestException("Dispatcher does not have access to club", input.clubId());
        }

        CreateBranchCommand command = CreateBranchCommand.builder()
                .clubId(input.clubId())
                .name(input.name())
                .address(input.address())
                .build();

        UUID branchId = branchPort.create(command);

        return new DispatcherBranchCreateOutput(branchId);
    }

    @Transactional(readOnly = true)
    public List<DispatcherBranchesOutput> getDispatcherBranches(UUID dispatcherId) {
        Collection<UUID> clubIds = dispatcherClubService.getAll(dispatcherId);
        if (clubIds.isEmpty()) {
            return List.of();
        }

        Collection<BranchDto> branches = branchPort.findByClubIds(clubIds);

        return branches.stream()
                .map(b -> DispatcherBranchesOutput.builder()
                        .branchId(b.id())
                        .name(b.name())
                        .address(b.address())
                        .clubId(b.clubId())
                        .build())
                .toList();
    }

    @Transactional
    public void deleteBranch(UUID dispatcherId, UUID branchId) {
        BranchDto branch = branchPort.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found", branchId));

        boolean isDispatcherBranch = dispatcherClubService.getAll(dispatcherId)
                .stream()
                .anyMatch(id -> id.equals(branch.clubId()));

        if (!isDispatcherBranch) {
            throw new BadRequestException("Dispatcher does not have access to branch", branchId);
        }

        branchPort.delete(branchId);
    }

    public boolean verifyBranchBelongsToDispatcher(UUID dispatcherId, UUID branchId) {
        BranchDto branch = branchPort.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found", branchId));

        return dispatcherClubService.getAll(dispatcherId)
                .stream()
                .anyMatch(id -> id.equals(branch.clubId()));
    }
}
