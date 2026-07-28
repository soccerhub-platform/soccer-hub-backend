package kz.edu.soccerhub.coach.infrastructure;

import kz.edu.soccerhub.coach.domain.model.TrainingSession;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus;
import kz.edu.soccerhub.coach.domain.repository.TrainingSessionRepository;
import kz.edu.soccerhub.common.dto.trial.TrialSessionContext;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.TrialSessionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TrainingSessionTrialAdapter implements TrialSessionPort {

    private final TrainingSessionRepository repository;

    @Override
    public TrialSessionContext getBookableSession(UUID sessionId) {
        TrainingSession session = repository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException(
                        "Training session not found",
                        sessionId
                ));

        if (session.getStatus() != TrainingSessionStatus.PLANNED) {
            throw new BadRequestException(
                    "Trial can only be booked for a planned session",
                    sessionId
            );
        }

        if (!session.getScheduledStartAt().isAfter(LocalDateTime.now())) {
            throw new BadRequestException(
                    "Trial cannot be booked for a started session",
                    sessionId
            );
        }

        return TrialSessionContext.builder()
                .sessionId(session.getId())
                .groupId(session.getGroupId())
                .coachId(session.getCoachId())
                .locationId(session.getLocationId())
                .startsAt(session.getScheduledStartAt())
                .endsAt(session.getScheduledEndAt())
                .status(session.getStatus())
                .build();
    }

    @Override
    public TrialSessionContext getSessionDetails(UUID sessionId) {
        TrainingSession session = repository.findById(sessionId)
                .orElseThrow(() -> new NotFoundException(
                        "Training session not found",
                        sessionId
                ));

        return toContext(session);
    }

    @Override
    public Map<UUID, TrialSessionContext> getSessionDetails(
            Collection<UUID> sessionIds
    ) {
        if (sessionIds.isEmpty()) {
            return Map.of();
        }

        return repository.findAllById(sessionIds).stream()
                .collect(Collectors.toMap(
                        TrainingSession::getId,
                        this::toContext
                ));
    }

    private TrialSessionContext toContext(TrainingSession session) {
        return TrialSessionContext.builder()
                .sessionId(session.getId())
                .groupId(session.getGroupId())
                .coachId(session.getCoachId())
                .locationId(session.getLocationId())
                .startsAt(session.getScheduledStartAt())
                .endsAt(session.getScheduledEndAt())
                .status(session.getStatus())
                .build();
    }
}