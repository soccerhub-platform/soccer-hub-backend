package kz.edu.soccerhub.dispatcher.application.service;

import kz.edu.soccerhub.common.dto.coach.CoachDto;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.dispatcher.application.dto.branch.DispatcherBranchesOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DispatcherCoachService {

    private final CoachPort coachPort;
    private final DispatcherBranchService dispatcherBranchService;

    @Transactional(readOnly = true)
    public Page<CoachDto> getCoaches(UUID dispatcherId, UUID branchId, Pageable pageable) {
        boolean isBranchBelongsToDispatcher = dispatcherBranchService.verifyBranchBelongsToDispatcher(dispatcherId, branchId);
        if (!isBranchBelongsToDispatcher) {
            throw new BadRequestException("Dispatcher does not have access to branch", branchId);
        }

        return coachPort.getCoaches(Set.of(branchId), pageable);
    }

}
