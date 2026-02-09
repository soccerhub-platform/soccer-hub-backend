package kz.edu.soccerhub.coach.application.service;

import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.coach.domain.model.CoachProfile;
import kz.edu.soccerhub.coach.domain.model.enums.CoachStatus;
import kz.edu.soccerhub.coach.domain.repository.CoachProfileRepository;
import kz.edu.soccerhub.common.dto.coach.CoachCreateCommand;
import kz.edu.soccerhub.common.dto.coach.CoachDto;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.CoachPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoachService implements CoachPort {

    private final CoachProfileRepository coachProfileRepository;
    private final CoachBranchService coachBranchService;

    @Transactional
    public UUID create(CoachCreateCommand command) {
        CoachProfile profile = CoachProfile.builder()
                .id(command.id())
                .firstName(command.firstName())
                .lastName(command.lastName())
                .birthDate(command.birthDate())
                .phone(command.phone())
                .email(command.email())
                .status(CoachStatus.ACTIVE)
                .build();

        coachProfileRepository.save(profile);
        return profile.getId();
    }

    @Transactional
    public void assignToBranch(@NotNull UUID coachId, @NotNull UUID branchId) {
        Optional<CoachProfile> coachProfileOptional = coachProfileRepository.findById(coachId);
        if (coachProfileOptional.isEmpty()) {
            throw new NotFoundException("Coach not found", coachId);
        }
        if (coachProfileOptional.get().getStatus() != CoachStatus.ACTIVE) {
            throw new NotFoundException("Coach is not active", coachId);
        }
        coachBranchService.assignToBranch(coachId, branchId);
    }

    @Override
    public void unassignFromBranch(UUID coachId, UUID branchId) {
        boolean exists = coachProfileRepository.existsById(coachId);
        if (!exists) {
            throw new NotFoundException("Coach not found", coachId);
        }
        coachBranchService.unassignFromBranch(coachId, branchId);
    }

    @Transactional(readOnly = true)
    public CoachDto getCoach(UUID coachId) {
        return findById(coachId)
                .orElseThrow(() -> new NotFoundException("Coach not found", coachId));
    }

    @Override
    public Collection<CoachDto> getCoaches(Set<UUID> coachIds) {
        return coachProfileRepository.findAllById(coachIds)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public boolean verifyCoach(UUID coachId) {
        return coachProfileRepository.existsById(coachId);
    }

    @Transactional(readOnly = true)
    public Optional<CoachDto> findById(UUID coachId) {
        return coachProfileRepository.findById(coachId)
                .map(this::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CoachDto> getCoaches(Set<UUID> branchIds, Pageable pageable) {
        return coachProfileRepository
                .findAccessibleCoaches(branchIds, pageable)
                .map(this::toDto);
    }

    @Override
    @Transactional
    public void enableCoach(UUID coachId) {
        coachProfileRepository.findById(coachId)
                .ifPresent(coachProfile -> coachProfile.setStatus(CoachStatus.ACTIVE));
    }

    @Override
    public void disableCoach(UUID coachId) {
        coachProfileRepository.findById(coachId)
                .ifPresent(coachProfile -> coachProfile.setStatus(CoachStatus.INACTIVE));
    }

    private CoachDto toDto(@NotNull CoachProfile coachProfile) {
        return CoachDto.builder()
                .id(coachProfile.getId())
                .firstName(coachProfile.getFirstName())
                .lastName(coachProfile.getLastName())
                .phone(coachProfile.getPhone())
                .email(coachProfile.getEmail())
                .active(coachProfile.getStatus() == CoachStatus.ACTIVE)
                .build();
    }
}