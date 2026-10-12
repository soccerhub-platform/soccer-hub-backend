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
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import kz.edu.soccerhub.dispatcher.application.dto.coach.*;
import kz.edu.soccerhub.dispatcher.application.dto.admin.*;
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
                                                     @PathVariable("branchId") UUID branchId,
                                                     @PageableDefault Pageable pageable) {
        final UUID dispatcherId = UUID.fromString(jwt.getSubject());
        Page<CoachDto> coaches = dispatcherCoachService.getCoaches(dispatcherId, branchId, pageable);
        return ResponseEntity.ok().body(coaches);
    }

    @GetMapping
    public Page<DispatcherCoachOutput> list(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) UUID branchId,
            @RequestParam(defaultValue="") String search,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size) {
        return dispatcherCoachService.list(UUID.fromString(jwt.getSubject()),branchId,search,page,size);
    }
    @PostMapping
    public java.util.Map<String,Object> create(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody DispatcherCoachInput input) {
        return dispatcherCoachService.create(UUID.fromString(jwt.getSubject()),input);
    }
    @PutMapping("/{id}")
    public void update(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody DispatcherCoachInput input) {dispatcherCoachService.update(UUID.fromString(jwt.getSubject()),id,input);}
    @PatchMapping("/{id}/status")
    public void status(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody DispatcherAdminChangeStatusInput input) {dispatcherCoachService.status(UUID.fromString(jwt.getSubject()),id,input.active());}
    @PutMapping("/{id}/branches/{branchId}")
    public void assign(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@PathVariable UUID branchId) {dispatcherCoachService.assignment(UUID.fromString(jwt.getSubject()),id,branchId,true);}
    @DeleteMapping("/{id}/branches/{branchId}")
    public void unassign(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@PathVariable UUID branchId) {dispatcherCoachService.assignment(UUID.fromString(jwt.getSubject()),id,branchId,false);}
    @GetMapping("/{id}/permissions")
    public kz.edu.soccerhub.common.dto.auth.EmployeeAccessDto permissions(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) {return dispatcherCoachService.permissions(UUID.fromString(jwt.getSubject()),id);}
    @PutMapping("/{id}/permissions")
    public void permissions(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody DispatcherEmployeePermissionsInput input) {dispatcherCoachService.permissions(UUID.fromString(jwt.getSubject()),id,input);}
    @PostMapping("/{id}/reset-password")
    public java.util.Map<String,String> reset(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) {return dispatcherCoachService.resetPassword(UUID.fromString(jwt.getSubject()),id);}
}
