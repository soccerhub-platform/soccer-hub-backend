package kz.edu.soccerhub.dispacher.application.service;

import jakarta.validation.Valid;
import kz.edu.soccerhub.common.domain.enums.Role;
import kz.edu.soccerhub.common.dto.admin.AdminCreateCommand;
import kz.edu.soccerhub.common.dto.admin.AdminCreateCommandOutput;
import kz.edu.soccerhub.common.dto.auth.RegisterCommand;
import kz.edu.soccerhub.common.dto.auth.RegisterCommandOutput;
import kz.edu.soccerhub.common.dto.branch.BranchDto;
import kz.edu.soccerhub.common.dto.branch.CreateBranchCommand;
import kz.edu.soccerhub.common.dto.client.ClientCreateCommand;
import kz.edu.soccerhub.common.dto.client.ClientCreateCommandOutput;
import kz.edu.soccerhub.common.dto.club.ClubDto;
import kz.edu.soccerhub.common.dto.club.CreateClubCommand;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.port.*;
import kz.edu.soccerhub.dispacher.application.dto.*;
import kz.edu.soccerhub.dispacher.domain.model.DispatcherProfile;
import kz.edu.soccerhub.dispacher.domain.repository.DispatcherProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DispatcherService {

    private final DispatcherProfileRepository repository;
    private final DispatcherClubService dispatcherClubService;
    private final PasswordGenerator passwordGenerator;
    private final AuthPort authPort;
    private final AdminPort adminPort;
    private final ClientPort clientPort;
    private final ClubPort clubPort;
    private final BranchPort branchPort;

    @Transactional
    public DispatcherRegisterOutput register(@Valid DispatcherRegisterInput input) {
        String tempPassword = passwordGenerator.generate();

        RegisterCommand registerCommand = RegisterCommand.builder()
                .email(input.email())
                .password(tempPassword)
                .roles(Set.of(Role.DISPATCHER))
                .build();

        RegisterCommandOutput registrationResult = authPort.register(registerCommand);

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
    public DispatcherClubCreateOutput createClub(UUID dispatcherId, DispatcherClubCreateInput input) {

        UUID clubId = clubPort.create(
                CreateClubCommand.builder()
                        .name(input.name())
                        .slug(input.slug())
                        .email(input.email())
                        .phone(input.phone())
                        .address(input.address())
                        .build()
        );

        dispatcherClubService.attachDispatcherToClub(dispatcherId, clubId);

        return new DispatcherClubCreateOutput(clubId);
    }

    @Transactional
    public DispatcherBranchCreateOutput createBranch(UUID dispatcherId, DispatcherBranchCreateInput input) {
        boolean isExist = clubPort.isExist(input.clubId());
        if (!isExist) {
            throw new BadRequestException("Club does not exist", input.clubId());
        }

        boolean hasAccess = dispatcherClubService.hasAccess(dispatcherId, input.clubId());
        if (!hasAccess) {
            throw new BadRequestException("Dispatcher does not have access to club", input.clubId());
        }

        CreateBranchCommand command = CreateBranchCommand.builder()
                .clubId(input.clubId())
                .name(input.name())
                .address(input.address())
                .build();

        UUID branchId = branchPort.create(command);

        return new DispatcherBranchCreateOutput(branchId);
    }

    @Transactional
    public DispatcherAdminRegisterOutput registerAdmin(DispatcherAdminRegisterInput input) {
        String tempPassword = passwordGenerator.generate(6);

        RegisterCommand registerCommand = RegisterCommand.builder()
                .email(input.email())
                .password(tempPassword)
                .roles(Set.of(Role.ADMIN))
                .build();

        RegisterCommandOutput registerResult = authPort.register(registerCommand);

        AdminCreateCommand adminCommand = AdminCreateCommand.builder()
                .userId(registerResult.id())
                .firstName(input.firstName())
                .lastName(input.lastName())
                .phone(input.phone())
                .branchId(input.assignedBranch())
                .build();

        AdminCreateCommandOutput adminResult = adminPort.create(adminCommand);

        return new DispatcherAdminRegisterOutput(
                adminResult.adminId(),
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

    @Transactional(readOnly = true)
    public DispatcherClubsOutput getDispatcherClubs(UUID dispatcherId) {
        Collection<UUID> clubIds = dispatcherClubService.getClubs(dispatcherId);
        Collection<ClubDto> clubs = clubPort.findAllByIds(clubIds);

        List<DispatcherClubsOutput.DispatcherClubDto> result = clubs.stream()
                .map(c -> new DispatcherClubsOutput.DispatcherClubDto(
                        c.id(),
                        c.name(),
                        c.slug()
                ))
                .toList();

        return new DispatcherClubsOutput(result);
    }

    @Transactional(readOnly = true)
    public DispatcherBranchesOutput getDispatcherBranches(UUID dispatcherId) {
        Collection<UUID> clubIds = dispatcherClubService.getClubs(dispatcherId);
        if (clubIds.isEmpty()) {
            return new DispatcherBranchesOutput(List.of());
        }

        Collection<BranchDto> branches = branchPort.findByClubIds(clubIds);

        List<DispatcherBranchesOutput.DispatcherBranchDto> result = branches.stream()
                .map(b -> new DispatcherBranchesOutput.DispatcherBranchDto(
                        b.id(),
                        b.name(),
                        b.address()
                ))
                .toList();

        return new DispatcherBranchesOutput(result);
    }

}
