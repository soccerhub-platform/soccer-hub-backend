package kz.edu.soccerhub.dto;

import lombok.Builder;

@Builder
public record TeamDto(
    String name,
    String city,
    String country,
    String coachName,
    String coachPhone,
    String coachEmail,
    String logoUrl
) {
}
