package kz.edu.soccerhub.dispacher.application.dto.admin;

import lombok.Builder;

@Builder
public record DispatcherAdminUpdateInput(
        String firstName,
        String lastName,
        String phone

) {
}
