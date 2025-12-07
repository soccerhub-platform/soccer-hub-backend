package kz.edu.soccerhub.dispatcher.application.service;

import jakarta.validation.Valid;
import kz.edu.soccerhub.common.domain.enums.Role;
import kz.edu.soccerhub.common.dto.auth.AuthRegisterCommand;
import kz.edu.soccerhub.common.dto.auth.AuthRegisterCommandOutput;
import kz.edu.soccerhub.common.dto.client.ClientCreateCommand;
import kz.edu.soccerhub.common.dto.client.ClientCreateCommandOutput;
import kz.edu.soccerhub.common.port.AuthPort;
import kz.edu.soccerhub.common.port.ClientPort;
import kz.edu.soccerhub.dispatcher.application.dto.DispatcherClientRegisterInput;
import kz.edu.soccerhub.dispatcher.application.dto.DispatcherClientRegisterOutput;
import kz.edu.soccerhub.dispatcher.application.dto.DispatcherRegisterInput;
import kz.edu.soccerhub.dispatcher.application.dto.DispatcherRegisterOutput;
import kz.edu.soccerhub.dispatcher.domain.model.DispatcherProfile;
import kz.edu.soccerhub.dispatcher.domain.repository.DispatcherProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class DispatcherService {

    private final DispatcherProfileRepository repository;
    private final PasswordGenerator passwordGenerator;
    private final AuthPort authPort;
    private final ClientPort clientPort;

    @Transactional
    public DispatcherRegisterOutput register(@Valid DispatcherRegisterInput input) {
        String tempPassword = passwordGenerator.generate();

        AuthRegisterCommand authRegisterCommand = AuthRegisterCommand.builder()
                .email(input.email())
                .password(tempPassword)
                .roles(Set.of(Role.DISPATCHER))
                .build();

        AuthRegisterCommandOutput registrationResult = authPort.register(authRegisterCommand);

        repository.save(DispatcherProfile.builder()
                .id(registrationResult.id())
                .firstName(input.firstName())
                .lastName(input.lastName())
                .phone(input.phone())
                .build());

        return new DispatcherRegisterOutput(
                registrationResult.id(),
                tempPassword
        );
    }

    @Transactional
    public DispatcherClientRegisterOutput registerClient(DispatcherClientRegisterInput input) {
        ClientCreateCommand clientCreateCommand = ClientCreateCommand.builder()
                .branchId(input.branchId())
                .firstName(input.firstName())
                .lastName(input.lastName())
                .phone(input.phoneNumber())
                .source(input.source())
                .comments(input.comments())
                .build();

        ClientCreateCommandOutput clientCreateCommandOutput = clientPort.create(clientCreateCommand);
        return new DispatcherClientRegisterOutput(clientCreateCommandOutput.clientId());
    }

}
