package kz.edu.soccerhub.coach.application.service;

import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.auth.domain.repository.AppUserRepo;
import kz.edu.soccerhub.coach.domain.model.CoachBranch;
import kz.edu.soccerhub.coach.domain.model.CoachProfile;
import kz.edu.soccerhub.coach.domain.model.CoachStatusHistory;
import kz.edu.soccerhub.coach.domain.model.TrainingSession;
import kz.edu.soccerhub.coach.domain.model.enums.CoachStatus;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus;
import kz.edu.soccerhub.coach.domain.repository.CoachBranchRepository;
import kz.edu.soccerhub.coach.domain.repository.CoachProfileRepository;
import kz.edu.soccerhub.coach.domain.repository.CoachStatusHistoryRepository;
import kz.edu.soccerhub.coach.domain.repository.TrainingSessionRepository;
import kz.edu.soccerhub.common.dto.coach.CoachCreateCommand;
import kz.edu.soccerhub.common.dto.coach.CoachDto;
import kz.edu.soccerhub.common.dto.coach.CoachSessionAdminView;
import kz.edu.soccerhub.common.dto.coach.CoachStatusHistoryDto;
import kz.edu.soccerhub.common.dto.coach.CoachUpdateCommand;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.CoachPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoachService implements CoachPort {

    private final CoachProfileRepository coachProfileRepository;
    private final CoachBranchService coachBranchService;
    private final CoachBranchRepository coachBranchRepository;
    private final TrainingSessionRepository trainingSessionRepository;
    private final CoachStatusHistoryRepository coachStatusHistoryRepository;
    private final AppUserRepo appUserRepo;

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

    @Override
    @Transactional
    public void update(CoachUpdateCommand command) {
        CoachProfile profile = coachProfileRepository.findById(command.coachId())
                .orElseThrow(() -> new NotFoundException("Coach not found", command.coachId()));

        profile.setFirstName(command.firstName().trim());
        profile.setLastName(command.lastName().trim());
        profile.setEmail(command.email().trim().toLowerCase());
        profile.setPhone(trimToNull(command.phone()));
        profile.setSpecialization(trimToNull(command.specialization()));

        appUserRepo.findById(command.coachId()).ifPresent(user -> user.setEmail(profile.getEmail()));
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

    @Override
    @Transactional(readOnly = true)
    public Set<UUID> getBranchIds(UUID coachId) {
        return coachBranchRepository.findAllByCoachId(coachId).stream()
                .map(CoachBranch::getBranchId)
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoachSessionAdminView> getSessions(
            Set<UUID> coachIds,
            Set<UUID> groupIds,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        if (coachIds.isEmpty() || groupIds.isEmpty()) {
            return List.of();
        }
        return trainingSessionRepository
                .findByCoachIdInAndGroupIdInAndSessionDateBetween(coachIds, groupIds, dateFrom, dateTo)
                .stream()
                .map(this::toSessionAdminView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoachSessionAdminView> getOverdueReportSessions(
            Set<UUID> coachIds,
            Set<UUID> groupIds,
            LocalDate beforeDate
    ) {
        if (coachIds.isEmpty() || groupIds.isEmpty()) {
            return List.of();
        }
        return trainingSessionRepository
                .findByCoachIdInAndGroupIdInAndSessionDateBeforeAndReportDoneFalse(coachIds, groupIds, beforeDate)
                .stream()
                .filter(session -> session.getStatus() != TrainingSessionStatus.CANCELLED)
                .map(this::toSessionAdminView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoachSessionAdminView> getReportedSessions(Set<UUID> coachIds, Set<UUID> groupIds) {
        if (coachIds.isEmpty() || groupIds.isEmpty()) {
            return List.of();
        }
        return trainingSessionRepository.findByCoachIdInAndGroupIdInAndReportDoneTrue(coachIds, groupIds)
                .stream()
                .map(this::toSessionAdminView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoachSessionAdminView> getUpcomingSessions(UUID coachId, LocalDate fromDate) {
        return trainingSessionRepository
                .findByCoachIdAndSessionDateGreaterThanEqualOrderBySessionDateAscScheduledStartAtAsc(coachId, fromDate)
                .stream()
                .filter(session -> session.getStatus() != TrainingSessionStatus.CANCELLED)
                .map(this::toSessionAdminView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public int countOverdueReports(UUID coachId, LocalDate beforeDate) {
        return trainingSessionRepository.countByCoachIdAndSessionDateBeforeAndReportDoneFalseAndStatusNot(
                coachId,
                beforeDate,
                TrainingSessionStatus.CANCELLED
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoachSessionAdminView> getReportedSessions(UUID coachId) {
        return trainingSessionRepository.findByCoachIdAndReportDoneTrueOrderByUpdatedAtDesc(coachId)
                .stream()
                .map(this::toSessionAdminView)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoachStatusHistoryDto> getStatusHistory(UUID coachId) {
        return coachStatusHistoryRepository.findByCoachIdOrderByChangedAtDesc(coachId)
                .stream()
                .map(item -> new CoachStatusHistoryDto(
                        item.getStatus().name(),
                        item.getChangedAt(),
                        item.getChangedBy()
                ))
                .toList();
    }

    @Override
    @Transactional
    public void recordStatusHistory(UUID coachId, CoachStatus status, UUID changedBy) {
        coachStatusHistoryRepository.save(CoachStatusHistory.builder()
                .id(UUID.randomUUID())
                .coachId(coachId)
                .status(status)
                .changedAt(LocalDateTime.now())
                .changedBy(changedBy)
                .build());
    }

    private CoachDto toDto(@NotNull CoachProfile coachProfile) {
        return CoachDto.builder()
                .id(coachProfile.getId())
                .firstName(coachProfile.getFirstName())
                .lastName(coachProfile.getLastName())
                .phone(coachProfile.getPhone())
                .email(coachProfile.getEmail())
                .specialization(coachProfile.getSpecialization())
                .active(coachProfile.getStatus() == CoachStatus.ACTIVE)
                .build();
    }

    private CoachSessionAdminView toSessionAdminView(TrainingSession session) {
        return new CoachSessionAdminView(
                session.getId(),
                session.getCoachId(),
                session.getGroupId(),
                session.getSessionDate(),
                session.getScheduledStartAt(),
                session.getScheduledEndAt(),
                session.getStatus().name(),
                session.isReportDone(),
                session.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
