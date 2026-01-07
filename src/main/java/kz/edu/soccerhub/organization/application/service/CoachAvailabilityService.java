package kz.edu.soccerhub.organization.application.service;

import kz.edu.soccerhub.common.port.CoachAvailabilityPort;
import kz.edu.soccerhub.organization.application.dto.CoachBusySlotView;
import kz.edu.soccerhub.organization.domain.model.enums.ScheduleStatus;
import kz.edu.soccerhub.organization.domain.repository.GroupScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CoachAvailabilityService implements CoachAvailabilityPort {

    private final GroupScheduleRepository groupScheduleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CoachBusySlotView> getCoachAvailability(
            UUID coachId,
            LocalDate from,
            LocalDate to
    ) {

        return groupScheduleRepository
                .findByCoachIdAndStatusAndEndDateGreaterThanEqualAndStartDateLessThanEqual(
                        coachId,
                        ScheduleStatus.ACTIVE,
                        from,
                        to
                )
                .stream()
                .map(s -> CoachBusySlotView.builder()
                        .scheduleId(s.getId())
                        .groupId(s.getGroupId())
                        .dayOfWeek(s.getDayOfWeek())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .startDate(s.getStartDate())
                        .endDate(s.getEndDate())
                        .build())
                .toList();
    }
}