package kz.edu.soccerhub.admin.application.adapter;

import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.admin.application.dto.CreateAdminInput;
import kz.edu.soccerhub.admin.application.service.AdminService;
import kz.edu.soccerhub.common.port.AdminPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AdminAdapter implements AdminPort {

    private final AdminService adminService;

    @Override
    public UUID create(@NotNull UUID userId, String firstName, String lastName, String phone, UUID branchId) {
        return adminService.create(
                CreateAdminInput.builder()
                        .userId(userId)
                        .firstName(firstName)
                        .lastName(lastName)
                        .phone(phone)
                        .assignedBranch(branchId)
                        .build())
                .id();
    }
}
