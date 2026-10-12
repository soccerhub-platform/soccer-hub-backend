package kz.edu.soccerhub.dispatcher.application.service;

import kz.edu.soccerhub.common.dto.branch.BranchDto;
import kz.edu.soccerhub.common.dto.branch.CreateBranchCommand;
import kz.edu.soccerhub.common.dto.branch.UpdateBranchDetailsCommand;
import kz.edu.soccerhub.common.dto.branch.ChangeBranchArchiveCommand;
import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.port.AdminPort;
import kz.edu.soccerhub.dispatcher.application.dto.branch.DispatcherBranchUpdateInput;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.BranchPort;
import kz.edu.soccerhub.dispatcher.application.dto.branch.DispatcherBranchCreateInput;
import kz.edu.soccerhub.dispatcher.application.dto.branch.DispatcherBranchCreateOutput;
import kz.edu.soccerhub.dispatcher.application.dto.branch.DispatcherBranchesOutput;
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
    private final AdminPort adminPort;
    private final kz.edu.soccerhub.common.port.BranchHistoryPort historyPort;

    @Transactional
    public DispatcherBranchCreateOutput createBranch(UUID dispatcherId, DispatcherBranchCreateInput input) {

        boolean hasAccess = dispatcherClubService.hasAccess(dispatcherId, input.clubId());
        if (!hasAccess) {
            throw new BadRequestException("Dispatcher does not have access to club", input.clubId());
        }

        validateManager(dispatcherId, input.managerAdminId());

        CreateBranchCommand command = CreateBranchCommand.builder()
                .clubId(input.clubId())
                .name(input.name())
                .address(input.address())
                .phone(input.phone())
                .email(input.email())
                .managerAdminId(input.managerAdminId())
                .workingHours(input.workingHours())
                .build();

        UUID branchId = branchPort.create(command);
        if (input.managerAdminId() != null) adminPort.assignToBranch(input.managerAdminId(), branchId);

        return new DispatcherBranchCreateOutput(branchId);
    }

    @Transactional(readOnly = true)
    public List<DispatcherBranchesOutput> getDispatcherBranches(UUID dispatcherId) {
        Collection<UUID> clubIds = dispatcherClubService.getAll(dispatcherId);
        if (clubIds.isEmpty()) {
            return List.of();
        }

        Collection<BranchDto> branches = branchPort.findByClubIds(clubIds);

        Map<UUID, AdminDto> admins = adminPort.findAllByDispatcherId(dispatcherId).stream()
                .collect(Collectors.toMap(AdminDto::id, Function.identity()));
        return branches.stream()
                .map(b -> DispatcherBranchesOutput.builder()
                        .branchId(b.id())
                        .name(b.name())
                        .address(b.address())
                        .clubId(b.clubId())
                        .active(b.active())
                        .phone(b.phone())
                        .email(b.email())
                        .managerAdminId(b.managerAdminId())
                        .managerName(managerName(admins.get(b.managerAdminId())))
                        .workingHours(b.workingHours())
                        .archivedAt(b.archivedAt())
                        .build())
                .toList();
    }

    @Transactional
    public void updateBranch(UUID dispatcherId, UUID branchId, DispatcherBranchUpdateInput input) {
        requireOwnedBranch(dispatcherId, branchId);
        validateManager(dispatcherId, input.managerAdminId());
        branchPort.updateDetails(UpdateBranchDetailsCommand.builder()
                .branchId(branchId).name(input.name()).address(input.address())
                .phone(input.phone()).email(input.email()).managerAdminId(input.managerAdminId())
                .workingHours(input.workingHours()).build());
        if (input.managerAdminId() != null) adminPort.assignToBranch(input.managerAdminId(), branchId);
    }

    @Transactional
    public void archiveBranch(UUID dispatcherId, UUID branchId, boolean archived) {
        requireOwnedBranch(dispatcherId, branchId);
        branchPort.changeArchive(new ChangeBranchArchiveCommand(branchId, archived));
    }

    public BranchDto requireOwnedBranch(UUID dispatcherId, UUID branchId) {
        BranchDto branch = branchPort.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found", branchId));
        if (!dispatcherClubService.hasAccess(dispatcherId, branch.clubId())) {
            throw new BadRequestException("Dispatcher does not have access to branch", branchId);
        }
        return branch;
    }

    public void requireActiveBranch(UUID dispatcherId, UUID branchId) {
        if (!requireOwnedBranch(dispatcherId, branchId).active()) {
            throw new BadRequestException("Филиал в архиве. Сначала восстановите его");
        }
    }

    private void validateManager(UUID dispatcherId, UUID managerId) {
        if (managerId == null) return;
        AdminDto manager = adminPort.findById(managerId)
                .orElseThrow(() -> new NotFoundException("Руководитель не найден", managerId));
        if (!dispatcherId.equals(manager.dispatcherId()) || !manager.isActive()) {
            throw new BadRequestException("Выберите действующего администратора вашей компании");
        }
    }

    private static String managerName(AdminDto admin) {
        if (admin == null) return null;
        return (java.util.Objects.toString(admin.firstName(), "") + " "
                + java.util.Objects.toString(admin.lastName(), "")).trim();
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

    public boolean verifyBranchBelongsToDispatcher(UUID dispatcherId, Collection<UUID> branchesId) {
        Collection<BranchDto> branches = branchPort.findAllByIds(branchesId);
        if (branches.isEmpty()) {
            throw new NotFoundException("Branch not found", branchesId);
        }

        return dispatcherClubService.getAll(dispatcherId)
                .stream()
                .anyMatch(clubId -> branches.stream().anyMatch(branch -> branch.clubId().equals(clubId)));
    }

    public boolean verifyBranchBelongsToDispatcher(UUID dispatcherId, UUID branchId) {
        BranchDto branch = branchPort.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found", branchId));

        return dispatcherClubService.getAll(dispatcherId)
                .stream()
                .anyMatch(clubId -> branch.clubId().equals(clubId));
    }
    public org.springframework.data.domain.Page<kz.edu.soccerhub.common.dto.branch.BranchHistoryOutput> history(UUID user,UUID branch,int page,int size) {
        requireOwnedBranch(user,branch);return historyPort.history(branch,page,size);
    }
}
