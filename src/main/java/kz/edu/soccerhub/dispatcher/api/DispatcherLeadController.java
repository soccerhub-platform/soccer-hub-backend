package kz.edu.soccerhub.dispatcher.api;

import jakarta.validation.Valid;
import kz.edu.soccerhub.common.dto.lead.LeadCreateOutput;
import kz.edu.soccerhub.dispatcher.application.dto.DispatcherLeadCreateInput;
import kz.edu.soccerhub.dispatcher.application.service.DispatcherLeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/dispatcher/leads")
@PreAuthorize("hasAuthority('DISPATCHER')")
@RequiredArgsConstructor
public class DispatcherLeadController {

    private final DispatcherLeadService dispatcherLeadService;

    @PostMapping
    public ResponseEntity<LeadCreateOutput> createLead(@RequestBody @Valid DispatcherLeadCreateInput input) {
        LeadCreateOutput output = dispatcherLeadService.createLead(input);
        return ResponseEntity
                .created(URI.create("/leads/" + output.leadId()))
                .body(output);
    }
}

