package kz.edu.soccerhub.organization.api;

import kz.edu.soccerhub.organization.application.dto.CoachBusySlotView;
import kz.edu.soccerhub.organization.application.service.CoachAvailabilityService;
import kz.edu.soccerhub.organization.application.service.GroupCoachService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/organization/groups")
@RequiredArgsConstructor
public class GroupCoachController {

    private final CoachAvailabilityService coachAvailabilityService;
    private final GroupCoachService groupCoachService;


    @GetMapping("/{groupId}/coaches/active")
    public Map<String, Object> getGroupCoaches(@PathVariable UUID groupId) {
        var activeCoaches = groupCoachService.getActiveCoaches(groupId);
        return Map.of("coaches", activeCoaches);
    }

    @GetMapping("/coaches/{coachId}/availability")
    public List<CoachBusySlotView> getCoachAvailability(
            @PathVariable UUID coachId,
            @RequestParam LocalDate from,
            @RequestParam LocalDate to
    ) {
        return coachAvailabilityService.getCoachAvailability(coachId, from, to);
    }
}