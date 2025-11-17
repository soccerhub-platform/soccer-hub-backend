package kz.edu.soccerhub.admin.application;

import jakarta.validation.Valid;
import kz.edu.soccerhub.admin.domain.model.AdminProfileEntity;
import kz.edu.soccerhub.admin.domain.repository.AdminProfileRepository;
import kz.edu.soccerhub.common.dto.admin.AdminCreateCommand;
import kz.edu.soccerhub.common.dto.admin.AdminCreateCommandOutput;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.AdminPort;
import kz.edu.soccerhub.common.port.BranchPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminService implements AdminPort {

    private final AdminProfileRepository adminProfileRepository;
    private final BranchPort branchPort;

    @Override
    public AdminCreateCommandOutput create(@Valid AdminCreateCommand command) {
        boolean isBranchExist = branchPort.isExist(command.branchId());
        if (!isBranchExist) {
            throw new NotFoundException("Branch not found", command.branchId());
        }

        AdminProfileEntity entity = AdminProfileEntity.builder()
                .id(command.userId())
                .firstName(command.firstName())
                .lastName(command.lastName())
                .phone(command.phone())
                .branch(command.branchId())
                .build();

        AdminProfileEntity saved = adminProfileRepository.save(entity);
        return new AdminCreateCommandOutput(saved.getId());
    }

}
