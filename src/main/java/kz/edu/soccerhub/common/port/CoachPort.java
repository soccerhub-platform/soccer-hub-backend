package kz.edu.soccerhub.common.port;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.coach.domain.model.enums.AccountStatus;
import kz.edu.soccerhub.coach.domain.model.enums.CoachStatusHistoryEventType;
import kz.edu.soccerhub.coach.domain.model.enums.WorkStatus;
import kz.edu.soccerhub.coach.application.dto.profile.CoachAvailabilityResponse;
import kz.edu.soccerhub.coach.application.dto.profile.CoachAvailabilityUpdateRequest;
import kz.edu.soccerhub.common.dto.coach.CoachCreateCommand;
import kz.edu.soccerhub.common.dto.coach.CoachDto;
import kz.edu.soccerhub.common.dto.coach.PlayerAttendanceRecordDto;
import kz.edu.soccerhub.common.dto.coach.CoachSessionAdminView;
import kz.edu.soccerhub.common.dto.coach.CoachStatusHistoryDto;
import kz.edu.soccerhub.common.dto.coach.CoachUpdateCommand;
import kz.edu.soccerhub.common.dto.coach.PlayerAttendanceRateDto;
import kz.edu.soccerhub.common.dto.coach.PlayerAttendanceSummaryDto;
import kz.edu.soccerhub.common.dto.coach.SessionAttendanceSummaryDto;
import kz.edu.soccerhub.coach.domain.model.enums.CoachStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface CoachPort {
    UUID create(@Valid CoachCreateCommand command);
    void update(@Valid CoachUpdateCommand command);
    void assignToBranch(@NotNull UUID coachId, @NotNull UUID branchId);
    void unassignFromBranch(@NotNull UUID coachId, @NotNull UUID branchId);
    Page<CoachDto> getCoaches(Set<UUID> branchId, Pageable pageable);
    CoachDto getCoach(UUID coachId);
    Collection<CoachDto> getCoaches(Set<UUID> coachIds);
    boolean verifyCoach(UUID coachId);
    void enableCoach(UUID coachId);
    void disableCoach(UUID coachId);
    void updateWorkStatus(UUID coachId, WorkStatus workStatus, LocalDate vacationFrom, LocalDate vacationTo, String reason);
    CoachAvailabilityResponse getAvailability(UUID coachId);
    CoachAvailabilityResponse updateAvailability(UUID coachId, CoachAvailabilityUpdateRequest request);
    Set<UUID> getBranchIds(UUID coachId);
    List<CoachSessionAdminView> getSessions(Set<UUID> coachIds, Set<UUID> groupIds, LocalDate dateFrom, LocalDate dateTo);
    List<CoachSessionAdminView> getOverdueReportSessions(Set<UUID> coachIds, Set<UUID> groupIds, LocalDate beforeDate);
    List<CoachSessionAdminView> getReportedSessions(Set<UUID> coachIds, Set<UUID> groupIds);
    List<CoachSessionAdminView> getUpcomingSessions(UUID coachId, LocalDate fromDate);
    int countOverdueReports(UUID coachId, LocalDate beforeDate);
    List<CoachSessionAdminView> getReportedSessions(UUID coachId);
    List<CoachStatusHistoryDto> getStatusHistory(UUID coachId);
    void recordStatusHistory(UUID coachId, CoachStatus status, UUID changedBy);
    List<PlayerAttendanceRateDto> getAttendanceRates(UUID groupId, Set<UUID> playerIds);
    List<PlayerAttendanceSummaryDto> getAttendanceSummaries(Set<UUID> playerIds);
    List<PlayerAttendanceRecordDto> getRecentAttendance(UUID playerId, int limit);
    List<SessionAttendanceSummaryDto> getSessionAttendanceSummaries(Set<UUID> sessionIds);

    void recordStatusHistory(
            UUID coachId,
            CoachStatus status,
            UUID changedBy,
            CoachStatusHistoryEventType eventType,
            AccountStatus previousAccountStatus,
            AccountStatus newAccountStatus,
            WorkStatus previousWorkStatus,
            WorkStatus newWorkStatus,
            String reason,
            LocalDate vacationFrom,
            LocalDate vacationTo
    );
}
