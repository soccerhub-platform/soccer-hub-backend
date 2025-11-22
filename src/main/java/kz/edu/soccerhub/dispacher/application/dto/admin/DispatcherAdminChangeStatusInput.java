package kz.edu.soccerhub.dispacher.application.dto.admin;

import lombok.Builder;

@Builder
public record DispatcherAdminChangeStatusInput(
        boolean active
) {
}
