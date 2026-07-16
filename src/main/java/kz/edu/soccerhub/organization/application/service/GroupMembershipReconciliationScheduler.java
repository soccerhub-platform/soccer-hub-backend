package kz.edu.soccerhub.organization.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupMembershipReconciliationScheduler {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Almaty");

    private final GroupMembershipReconciliationService reconciliationService;

    @EventListener(ApplicationReadyEvent.class)
    public void reconcileAfterStartup() {
        reconcileToday("startup");
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Almaty")
    public void reconcileDaily() {
        reconcileToday("scheduled");
    }

    private void reconcileToday(String trigger) {
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        GroupMembershipReconciliationService.Result result = reconciliationService.reconcile(today);
        log.info(
                "Group membership reconciliation completed: trigger={}, date={}, activated={}, completed={}",
                trigger,
                today,
                result.activated(),
                result.completed()
        );
    }
}
