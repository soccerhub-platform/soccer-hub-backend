package kz.edu.soccerhub.organization.api;

import kz.edu.soccerhub.common.dto.group.GroupScheduleDto;
import kz.edu.soccerhub.organization.application.dto.ScheduleSearchCriteria;
import kz.edu.soccerhub.organization.application.service.GroupScheduleService;
import kz.edu.soccerhub.organization.domain.model.enums.ScheduleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/organization/schedules")
public class GroupScheduleController {

    private final GroupScheduleService groupScheduleService;

    @GetMapping
    public ResponseEntity<List<GroupScheduleDto>> getSchedules(
            @RequestParam(value = "group-id", required = false) UUID groupId,
            @RequestParam(value = "coach-id", required = false) UUID coachId,
            @RequestParam(value = "branch-id", required = false) UUID branchId,
            @RequestParam(value = "from-date", required = false) String fromDate,
            @RequestParam(value = "to-date", required = false) String toDate,
            @RequestParam(value = "day-of-week", required = false) DayOfWeek dayOfWeek,
            @RequestParam(value = "status", required = false) ScheduleStatus status
    ) {
        ScheduleSearchCriteria criteria = ScheduleSearchCriteria.builder()
                .groupId(groupId)
                .coachId(coachId)
                .branchId(branchId)
                .fromDate(LocalDate.parse(fromDate))
                .toDate(LocalDate.parse(toDate))
                .dayOfWeek(dayOfWeek)
                .status(status)
                .build();
        return ResponseEntity.ok(
                groupScheduleService.getSchedules(criteria)
        );
    }


}