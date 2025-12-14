package kz.edu.soccerhub.coach.application.service;

import kz.edu.soccerhub.coach.application.dto.CoachBranchDto;
import kz.edu.soccerhub.coach.domain.model.CoachBranch;
import kz.edu.soccerhub.coach.domain.repository.CoachBranchRepository;
import kz.edu.soccerhub.common.dto.branch.BranchDto;
import kz.edu.soccerhub.common.dto.coach.CoachDto;
import kz.edu.soccerhub.common.port.BranchPort;
import kz.edu.soccerhub.common.port.CoachPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CoachBranchService {

    private final CoachBranchRepository coachBranchRepository;

    @Transactional(readOnly = true)
    public Set<UUID> getAccessibleBranchIds(Set<UUID> coachIds) {
        return coachBranchRepository.findAllByCoachIdIn(coachIds).stream()
                .map(CoachBranch::getBranchId)
                .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public Set<UUID> getAccessibleCoachIds(Set<UUID> branchIds) {
        return coachBranchRepository.findAllByBranchIdIn(branchIds).stream()
                .map(CoachBranch::getCoachId)
                .collect(Collectors.toSet());
    }

    @Transactional
    public void assignToBranch(UUID coachId, UUID branchId) {
        if (coachBranchRepository.existsByCoachIdAndBranchId(coachId, branchId)) {
            return;
        }

        CoachBranch cb = CoachBranch.builder()
                .id(UUID.randomUUID())
                .coachId(coachId)
                .branchId(branchId)
                .build();

        coachBranchRepository.save(cb);
    }

    @Transactional
    public void unassignFromBranch(UUID coachId, UUID branchId) {
        coachBranchRepository.findAllByCoachIdIn(Set.of(coachId)).stream()
                .filter(cb -> cb.getBranchId().equals(branchId))
                .findFirst()
                .ifPresent(coachBranchRepository::delete);
    }
}
