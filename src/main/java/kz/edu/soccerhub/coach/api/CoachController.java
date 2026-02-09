package kz.edu.soccerhub.coach.api;

import kz.edu.soccerhub.coach.application.service.CoachService;
import kz.edu.soccerhub.common.dto.coach.CoachDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/coaches")
@RequiredArgsConstructor
public class CoachController {

    private final CoachService coachService;

    @GetMapping("/{coachId}")
    public CoachDto getCoach(@PathVariable UUID coachId) {
        return coachService.getCoach(coachId);
    }

}
