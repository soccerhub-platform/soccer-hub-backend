package kz.edu.soccerhub.crm.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.ConflictException;
import kz.edu.soccerhub.crm.domain.model.enums.Gender;
import kz.edu.soccerhub.crm.domain.model.enums.LeadParticipantStage;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "lead_participants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class LeadParticipant extends AbstractAuditableEntity {

    @Id
    @Column(nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    private Gender gender;

    @Column(name = "experience", length = 100)
    private String experience;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "stage", length = 32)
    private LeadParticipantStage stage = LeadParticipantStage.NEW;

    @Column(name = "player_id")
    private UUID playerId;

    @Builder.Default
    @Column(name = "stage_changed_at")
    private LocalDateTime stageChangedAt = LocalDateTime.now();

    public void linkPlayer(UUID requestedPlayerId) {
        if (requestedPlayerId == null) {
            throw new BadRequestException("Player id is required");
        }

        if (playerId == null) {
            playerId = requestedPlayerId;
            return;
        }

        if (!Objects.equals(playerId, requestedPlayerId)) {
            throw new ConflictException(
                    "Lead participant is already linked to another player",
                    "LEAD_PARTICIPANT_PLAYER_CONFLICT",
                    Map.of(
                            "participantId", String.valueOf(id),
                            "currentPlayerId", playerId,
                            "requestedPlayerId", requestedPlayerId
                    )
            );
        }
    }

    public void startTrial() {
        transitionTo(LeadParticipantStage.TRIAL);
    }

    public void awaitContract() {
        transitionTo(LeadParticipantStage.CONTRACT);
    }

    public void awaitEnrollment() {
        transitionTo(LeadParticipantStage.ENROLLMENT);
    }

    public void awaitFirstPayment() {
        transitionTo(LeadParticipantStage.FIRST_PAYMENT);
    }

    public void completeOnFirstPayment() {
        transitionTo(LeadParticipantStage.COMPLETED);
    }

    public void markLost() {
        transitionTo(LeadParticipantStage.LOST);
    }

    private void transitionTo(LeadParticipantStage targetStage) {
        if (targetStage == null) {
            throw new BadRequestException("Target participant stage is required");
        }

        if (stage == null) {
            throw transitionConflict(targetStage, "Participant stage is not initialized");
        }

        if (stage == targetStage) {
            return;
        }

        if (!isTransitionAllowed(stage, targetStage)) {
            throw transitionConflict(targetStage, "Lead participant stage transition is not allowed");
        }

        stage = targetStage;
        stageChangedAt = LocalDateTime.now();
    }

    private boolean isTransitionAllowed(
            LeadParticipantStage currentStage,
            LeadParticipantStage targetStage
    ) {
        if (targetStage == LeadParticipantStage.LOST) {
            return currentStage != LeadParticipantStage.COMPLETED
                    && currentStage != LeadParticipantStage.LOST;
        }

        return switch (currentStage) {
            case NEW -> targetStage == LeadParticipantStage.TRIAL;
            case TRIAL -> targetStage == LeadParticipantStage.CONTRACT;
            case CONTRACT -> targetStage == LeadParticipantStage.ENROLLMENT;
            case ENROLLMENT -> targetStage == LeadParticipantStage.FIRST_PAYMENT;
            case FIRST_PAYMENT -> targetStage == LeadParticipantStage.COMPLETED;
            case COMPLETED, LOST -> false;
        };
    }

    private ConflictException transitionConflict(
            LeadParticipantStage targetStage,
            String message
    ) {
        return new ConflictException(
                message,
                "LEAD_PARTICIPANT_STAGE_CONFLICT",
                Map.of(
                        "participantId", String.valueOf(id),
                        "currentStage", stage == null ? "UNINITIALIZED" : stage.name(),
                        "targetStage", targetStage.name()
                )
        );
    }

    @PrePersist
    private void prepareForPersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }

        if (stage == null) {
            stage = LeadParticipantStage.NEW;
        }

        if (stageChangedAt == null) {
            stageChangedAt = LocalDateTime.now();
        }
    }
}