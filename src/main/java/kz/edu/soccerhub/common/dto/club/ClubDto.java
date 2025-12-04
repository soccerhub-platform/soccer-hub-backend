package kz.edu.soccerhub.common.dto.club;

import lombok.Builder;

import java.util.UUID;

@Builder
public record ClubDto(
        UUID id,
        String name,
        String slug,
        String phoneNumber,
        String email,
        String address,
        String logoUrl,
        String website,
        boolean active
        ) {
}
