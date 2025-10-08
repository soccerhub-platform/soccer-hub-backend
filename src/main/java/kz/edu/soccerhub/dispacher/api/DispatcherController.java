package kz.edu.soccerhub.dispacher.api;

import kz.edu.soccerhub.admin.domain.model.AdminProfileEntity;
import kz.edu.soccerhub.dispacher.application.dto.DispatcherAdminRegisterInput;
import kz.edu.soccerhub.dispacher.application.dto.DispatcherAdminRegisterOutput;
import kz.edu.soccerhub.dispacher.application.service.DispatcherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/dispatcher")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('DISPATCHER')")
public class DispatcherController {

    private final DispatcherService dispatcherService;

    @PostMapping(value = "/register/admin")
    public ResponseEntity<?> registerAdmin(@RequestBody DispatcherAdminRegisterInput input) {
       DispatcherAdminRegisterOutput output = dispatcherService.registerAdmin(input);
       return ResponseEntity
               .created(URI.create("/dispatcher/admin/" + output.userId()))
               .build();
    }

}
