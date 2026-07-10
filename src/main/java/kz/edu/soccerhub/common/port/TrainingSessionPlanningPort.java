package kz.edu.soccerhub.common.port;

import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;

public interface TrainingSessionPlanningPort {

    void materializeSchedules(Collection<UUID> scheduleIds);

    void resyncSchedules(Collection<UUID> oldScheduleIds, Collection<UUID> newScheduleIds, LocalDate fromDate);

    void cancelFuturePlannedSessions(Collection<UUID> scheduleIds, LocalDate fromDate, String reason);

    void reactivateScheduleCancelledSessions(Collection<UUID> scheduleIds, LocalDate fromDate);

    void materializeAllActiveSchedules();
}
