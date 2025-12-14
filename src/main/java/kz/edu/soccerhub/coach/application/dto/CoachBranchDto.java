package kz.edu.soccerhub.coach.application.dto;

import kz.edu.soccerhub.common.dto.branch.BranchDto;
import kz.edu.soccerhub.common.dto.coach.CoachDto;
import lombok.Builder;

import java.util.UUID;

@Builder
public record CoachBranchDto(
        UUID id,
        CoachDto coachDto,
        BranchDto branchDto
) {
}
