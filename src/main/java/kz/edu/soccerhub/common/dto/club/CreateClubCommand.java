package kz.edu.soccerhub.common.dto.club;

import lombok.Builder;

@Builder
public record CreateClubCommand(
        String name,
        String slug,
        String email,
        String phone,
        String website,
        String address
) {}