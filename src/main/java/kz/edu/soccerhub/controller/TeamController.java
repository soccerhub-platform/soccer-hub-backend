package kz.edu.soccerhub.controller;

import kz.edu.soccerhub.dto.TeamDto;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    @GetMapping
    @PreAuthorize("hasRole('LEAD') or hasRole('ADMIN')")
    public List<TeamDto> list() {
        return List.of(
            TeamDto.builder()
                .name("FC Barcelona")
                .city("Barcelona")
                .country("Spain")
                .coachName("Xavi Hernandez")
                .coachPhone("+34 123 456 789")
                .coachEmail("arsen@em.com")
                .logoUrl("https://example.com/logos/barcelona.png")
                .build()
        );
    }
}