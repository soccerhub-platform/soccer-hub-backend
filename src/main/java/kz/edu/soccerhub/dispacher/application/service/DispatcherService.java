package kz.edu.soccerhub.dispacher.application.service;

import kz.edu.soccerhub.common.domain.enums.Role;
import kz.edu.soccerhub.common.port.AdminPort;
import kz.edu.soccerhub.common.port.AuthPort;
import kz.edu.soccerhub.dispacher.application.dto.DispatcherAdminRegisterInput;
import kz.edu.soccerhub.dispacher.application.dto.DispatcherAdminRegisterOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DispatcherService {

    private final PasswordGenerator passwordGenerator;
    private final AuthPort authPort;
    private final AdminPort adminPort;

    @Transactional
    public DispatcherAdminRegisterOutput registerAdmin(DispatcherAdminRegisterInput input) {
        String tempPassword = passwordGenerator.generate(5);
        UUID registeredUserId = authPort.register(input.email(), tempPassword, Set.of(Role.ADMIN));
        adminPort.create(registeredUserId, input.firstName(), input.lastName(), input.phone(), input.assignedBranch());
        return new DispatcherAdminRegisterOutput(registeredUserId, tempPassword);
    }
}
