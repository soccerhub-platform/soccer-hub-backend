package kz.edu.soccerhub.dispatcher.api;

import jakarta.validation.Valid;
import kz.edu.soccerhub.common.dto.lead.LeadCreateOutput;
import kz.edu.soccerhub.dispatcher.application.dto.DispatcherLeadCreateInput;
import kz.edu.soccerhub.dispatcher.application.service.DispatcherLeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import kz.edu.soccerhub.common.dto.lead.*;
import kz.edu.soccerhub.dispatcher.application.service.DispatcherLeadWorkflowService;
import java.util.List;
import kz.edu.soccerhub.dispatcher.application.dto.operations.DispatcherLeadRoutingInput;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/dispatcher/leads")
@PreAuthorize("hasAuthority('DISPATCHER')")
@RequiredArgsConstructor
public class DispatcherLeadController {

    private final DispatcherLeadService dispatcherLeadService;
    private final DispatcherLeadWorkflowService workflow;
    @GetMapping("/{id}") public java.util.Map<String,Object> details(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) {var user=UUID.fromString(jwt.getSubject());return java.util.Map.of("lead",workflow.details(user,id),"branchId",workflow.requireLead(user,id));}
    @GetMapping("/{id}/activities") public List<LeadActivityOutput> history(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) {return workflow.history(UUID.fromString(jwt.getSubject()),id);}
    @PostMapping("/{id}/routing") public void route(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody DispatcherLeadRoutingInput c) {workflow.route(UUID.fromString(jwt.getSubject()),id,c.toCommand());}
    @PostMapping("/{id}/work") public void work(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody LeadWorkCommand c) {workflow.update(UUID.fromString(jwt.getSubject()),id,c);}


    @PostMapping
    public ResponseEntity<LeadCreateOutput> createLead(@AuthenticationPrincipal Jwt jwt, @RequestBody @Valid DispatcherLeadCreateInput input) {
        LeadCreateOutput output = dispatcherLeadService.createLead(UUID.fromString(jwt.getSubject()), input);
        return ResponseEntity
                .created(URI.create("/leads/" + output.primaryLeadId()))
                .body(output);
    }
}

