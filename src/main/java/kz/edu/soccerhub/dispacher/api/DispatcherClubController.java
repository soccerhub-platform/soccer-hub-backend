package kz.edu.soccerhub.dispacher.api;

import jakarta.validation.Valid;
import kz.edu.soccerhub.dispacher.application.dto.club.DispatcherClubCreateInput;
import kz.edu.soccerhub.dispacher.application.dto.club.DispatcherClubsOutput;
import kz.edu.soccerhub.dispacher.application.service.DispatcherClubService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(value = "/dispatcher/club")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('DISPATCHER')")
public class DispatcherClubController {

    private final DispatcherClubService dispatcherClubService;

    @PostMapping(value = "/create")
    public ResponseEntity<Void> createClub(@AuthenticationPrincipal Jwt jwt,
                                        @RequestBody @Valid DispatcherClubCreateInput input) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        var output = dispatcherClubService.createClub(dispatcherId, input);
        return ResponseEntity
                .created(URI.create("/club/" + output.clubId()))
                .build();
    }

    @GetMapping
    public ResponseEntity<?> getClubs(@AuthenticationPrincipal Jwt jwt) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        List<DispatcherClubsOutput> dispatcherClubs = dispatcherClubService.getDispatcherClubs(dispatcherId);
        return ResponseEntity.ok().body(
                Map.of("clubs", dispatcherClubs)
        );
    }

    @DeleteMapping(value = "/{clubId}")
    public ResponseEntity<Void> deleteClub(@AuthenticationPrincipal Jwt jwt,
                                           @PathVariable UUID clubId) {
        UUID dispatcherId = UUID.fromString(jwt.getSubject());
        dispatcherClubService.deleteClub(dispatcherId, clubId);
        return ResponseEntity.noContent().build();
    }

}
