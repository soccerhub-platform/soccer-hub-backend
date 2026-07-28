package kz.edu.soccerhub.common.port;

import java.util.UUID;
import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;
import kz.edu.soccerhub.crm.application.state.LeadEvent;

public interface TrialLeadPort {
    /**
     * Validates if the trial lead with the given ID is a participant in the trial session.
     *
     * @param leadId the ID of the trial lead to validate
     * @param studentId the ID of the trial student to validate
     * @throws IllegalArgumentException if the lead is not a participant in the trial session
     */
    void validateParticipant(UUID leadId, UUID studentId);

    /**
     * Validates if the trial lead with the given ID has been converted to a student.
     *
     * @param leadId the ID of the trial lead to validate
     * @param studentId the ID of the trial student to validate
     * @throws IllegalArgumentException if the lead has not been converted to a student
     */
    void validateConvertedStudent(UUID leadId, UUID studentId);

    /**
     * Retrieves the details of a participant in a trial session based on the lead ID and participant ID.
     *
     * @param leadId the ID of the trial lead
     * @param participantId the ID of the participant
     * @return the details of the participant
     */
    TrialBookingDetailsDto.Student getParticipantDetails(UUID leadId, UUID participantId);

    /**
     * Retrieves the details of a trial lead based on the lead ID.
     *
     * @param leadId the ID of the trial lead
     * @return the details of the trial lead
     */
    TrialBookingDetailsDto.Lead getDetails(UUID leadId);
}
