package kz.edu.soccerhub.common.dto.admin;

import lombok.Builder;

@Builder
public record AdminUpdateCommand(
        String firstName,
        String lastName,
        String email,
        String phone
) {
}
