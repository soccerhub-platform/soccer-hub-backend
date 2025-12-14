package kz.edu.soccerhub.dispatcher.api;

import kz.edu.soccerhub.common.dto.coach.CoachDto;
import kz.edu.soccerhub.dispatcher.application.service.DispatcherCoachService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/dispatcher/coach")
@PreAuthorize("hasAuthority('DISPATCHER')")
@RequiredArgsConstructor
public class DispatcherCoachController {

    private final DispatcherCoachService dispatcherCoachService;

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<Page<CoachDto>> getCoaches(@AuthenticationPrincipal Jwt jwt,
                                                     @PathVariable("branchId")UUID branchId,
                                                     @PageableDefault Pageable pageable) {
        final UUID dispatcherId = UUID.fromString(jwt.getSubject());
        Page<CoachDto> coaches = dispatcherCoachService.getCoaches(dispatcherId, branchId, pageable);
        return ResponseEntity.ok().body(coaches);
    }

}
