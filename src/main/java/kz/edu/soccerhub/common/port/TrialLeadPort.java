package kz.edu.soccerhub.common.port;

import java.util.UUID;
import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;

public interface TrialLeadPort {
    /**
     * Validates if the trial lead with the given ID is a participant in the trial session.
     *
     * @param leadId the ID of the trial lead to validate
     * @param studentId the ID of the trial student to validate
     * @throws IllegalArgumentException if the lead is not a participant in the trial session
     */
    void validateParticipant(UUID leadId, UUID studentId);

    void validateConvertedStudent(UUID leadId, UUID studentId);

    TrialBookingDetailsDto.Student getParticipantDetails(UUID leadId, UUID participantId);

    TrialBookingDetailsDto.Lead getDetails(UUID leadId);
}
