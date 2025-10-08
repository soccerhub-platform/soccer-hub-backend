package kz.edu.soccerhub.admin.application.service;

import jakarta.validation.Valid;
import kz.edu.soccerhub.admin.application.dto.CreateAdminInput;
import kz.edu.soccerhub.admin.application.dto.CreateAdminOutput;
import kz.edu.soccerhub.admin.domain.model.AdminProfileEntity;
import kz.edu.soccerhub.admin.domain.repository.AdminProfileRepository;
import kz.edu.soccerhub.common.domain.model.BranchEntity;
import kz.edu.soccerhub.common.domain.repository.BranchRepository;
import kz.edu.soccerhub.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminService {

    private final BranchRepository branchRepository;
    private final AdminProfileRepository adminProfileRepository;

    public CreateAdminOutput create(@Valid CreateAdminInput input) {
        BranchEntity branchEntity = branchRepository.findById(input.assignedBranch())
                .orElseThrow(() -> new NotFoundException("Branch not found", input.assignedBranch()));
        AdminProfileEntity entity = AdminProfileEntity.builder()
                .id(input.userId())
                .firstName(input.firstName())
                .lastName(input.lastName())
                .phone(input.phone())
                .branch(branchEntity)
                .build();

        AdminProfileEntity saved = adminProfileRepository.save(entity);
        return new CreateAdminOutput(saved.getId());
    }

}
