package kz.edu.soccerhub.dispatcher.api;

import jakarta.validation.Valid;
import kz.edu.soccerhub.dispatcher.application.dto.DispatcherClientRegisterInput;
import kz.edu.soccerhub.dispatcher.application.dto.DispatcherRegisterInput;
import kz.edu.soccerhub.dispatcher.application.dto.DispatcherRegisterOutput;
import kz.edu.soccerhub.dispatcher.application.service.DispatcherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/dispatcher")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('DISPATCHER')")
public class DispatcherController {

    private final DispatcherService dispatcherService;

    @PostMapping(value = "/register")
    public ResponseEntity<?> create(@RequestBody @Valid DispatcherRegisterInput input) {
        DispatcherRegisterOutput output = dispatcherService.register(input);
        return ResponseEntity
                .created(URI.create("/dispatcher/" + output.userId()))
                .body(Map.of("tempPassword", output.tempPassword()));
    }

    @PostMapping(value = "/register/client")
    public ResponseEntity<?> registerClient(@RequestBody DispatcherClientRegisterInput input) {
        var output = dispatcherService.registerClient(input);
        return ResponseEntity
                .created(URI.create("/client/" + output.clientId()))
                .build();
    }

}
