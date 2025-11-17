package kz.edu.soccerhub.client.api;

import kz.edu.soccerhub.client.application.ClientService;
import kz.edu.soccerhub.client.application.dto.ClientDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
@RequestMapping(value = "/client")
@PreAuthorize(value = "hasAuthority('CLIENT') or hasAuthority('ADMIN') or hasAuthority('DISPATCHER')")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @GetMapping(value = "/all", produces = "application/json")
    public ResponseEntity<Collection<ClientDto>> getAllClients() {
        return ResponseEntity.ok().body(clientService.getAll());
    }



}
