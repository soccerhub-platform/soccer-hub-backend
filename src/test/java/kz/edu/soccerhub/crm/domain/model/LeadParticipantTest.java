package kz.edu.soccerhub.crm.domain.model;

import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.ConflictException;
import kz.edu.soccerhub.crm.domain.model.enums.Gender;
import kz.edu.soccerhub.crm.domain.model.enums.LeadParticipantStage;
import kz.edu.soccerhub.crm.domain.model.enums.LeadSource;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.crm.domain.model.enums.LeadType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LeadParticipantTest {

    @Test
    void newParticipantStartsAtNewStage() {
        LeadParticipant participant = createParticipant();

        assertEquals(LeadParticipantStage.NEW, participant.getStage());
        assertNotNull(participant.getStageChangedAt());
        assertNull(participant.getPlayerId());
    }

    @Test
    void participantMovesThroughApprovedJourney() {
        LeadParticipant participant = createParticipant();

        participant.startTrial();
        assertEquals(LeadParticipantStage.TRIAL, participant.getStage());

        participant.awaitContract();
        assertEquals(LeadParticipantStage.CONTRACT, participant.getStage());

        participant.awaitEnrollment();
        assertEquals(LeadParticipantStage.ENROLLMENT, participant.getStage());

        participant.awaitFirstPayment();
        assertEquals(LeadParticipantStage.FIRST_PAYMENT, participant.getStage());

        participant.completeOnFirstPayment();
        assertEquals(LeadParticipantStage.COMPLETED, participant.getStage());
    }

    @Test
    void participantCannotSkipJourneyStage() {
        LeadParticipant participant = createParticipant();

        assertThrows(ConflictException.class, participant::awaitContract);
        assertEquals(LeadParticipantStage.NEW, participant.getStage());
    }

    @Test
    void completedParticipantCannotBeMarkedLost() {
        LeadParticipant participant = createParticipant();

        participant.startTrial();
        participant.awaitContract();
        participant.awaitEnrollment();
        participant.awaitFirstPayment();
        participant.completeOnFirstPayment();

        assertThrows(ConflictException.class, participant::markLost);
        assertEquals(LeadParticipantStage.COMPLETED, participant.getStage());
    }

    @Test
    void participantCanBeMarkedLostFromActiveStage() {
        LeadParticipant participant = createParticipant();

        participant.startTrial();
        participant.markLost();

        assertEquals(LeadParticipantStage.LOST, participant.getStage());
    }

    @Test
    void linkingSamePlayerTwiceIsIdempotent() {
        LeadParticipant participant = createParticipant();
        UUID playerId = UUID.randomUUID();

        participant.linkPlayer(playerId);

        assertDoesNotThrow(() -> participant.linkPlayer(playerId));
        assertEquals(playerId, participant.getPlayerId());
    }

    @Test
    void participantCannotBeLinkedToDifferentPlayer() {
        LeadParticipant participant = createParticipant();

        participant.linkPlayer(UUID.randomUUID());

        assertThrows(
                ConflictException.class,
                () -> participant.linkPlayer(UUID.randomUUID())
        );
    }

    @Test
    void playerIdIsRequired() {
        LeadParticipant participant = createParticipant();

        assertThrows(
                BadRequestException.class,
                () -> participant.linkPlayer(null)
        );
    }

    private LeadParticipant createParticipant() {
        Lead lead = Lead.builder()
                .id(UUID.randomUUID())
                .leadType(LeadType.CHILDREN)
                .primaryContactName("Айгуль Сарсенова")
                .primaryContactPhone("+77001234567")
                .source(LeadSource.OTHER)
                .status(LeadStatus.NEW)
                .branchId(UUID.randomUUID())
                .build();

        lead.addParticipant(
                "Алихан Сарсенов",
                LocalDate.of(2018, 5, 10),
                Gender.MALE,
                "BEGINNER"
        );

        return lead.getParticipants().getFirst();
    }
}