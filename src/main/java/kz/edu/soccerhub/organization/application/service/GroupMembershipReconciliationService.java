package kz.edu.soccerhub.organization.application.service;

import kz.edu.soccerhub.organization.domain.repository.GroupMembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GroupMembershipReconciliationService {

    private static final String SYSTEM_ACTOR = "system:membership-reconciliation";

    private final GroupMembershipRepository groupMembershipRepository;

    @Transactional
    public Result reconcile(LocalDate asOfDate) {
        LocalDateTime updatedAt = LocalDateTime.now();
        int completed = groupMembershipRepository.completeExpiredMemberships(
                asOfDate,
                updatedAt,
                SYSTEM_ACTOR
        );
        int activated = groupMembershipRepository.activateStartedMemberships(
                asOfDate,
                updatedAt,
                SYSTEM_ACTOR
        );
        return new Result(activated, completed);
    }

    public record Result(int activated, int completed) {
    }
}
