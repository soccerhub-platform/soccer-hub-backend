package kz.edu.soccerhub.payments.application;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import lombok.RequiredArgsConstructor;
@Component @RequiredArgsConstructor
@ConditionalOnProperty(prefix="app.dispatcher.debt-snapshots",name="enabled",havingValue="true",matchIfMissing=true)
public class DebtSnapshotScheduler {
 private final CompanyPaymentHistoryReader reader;
 @EventListener(ApplicationReadyEvent.class) @Scheduled(cron="0 5 0 * * *",zone="Asia/Almaty") public void capture() {reader.captureDebt();}
}
