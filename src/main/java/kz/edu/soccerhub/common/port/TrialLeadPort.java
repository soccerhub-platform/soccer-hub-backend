package kz.edu.soccerhub.common.port;

import java.util.Collection;
import java.util.Map;
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

    /**
     * Retrieves the details of multiple trial leads based on their IDs.
     *
     * @param leadIds the IDs of the trial leads
     * @return a map where the key is the lead ID and the value is the corresponding TrialBookingDetailsDto.Lead
     */
    Map<UUID, TrialBookingDetailsDto.Lead> getDetails(
            Collection<UUID> leadIds
    );

    /**
     * Retrieves the details of multiple participants in trial sessions based on their participant IDs.
     *
     * @param participantIds the IDs of the participants
     * @return a map where the key is the participant ID and the value is the corresponding TrialBookingDetailsDto.Student
     */
    Map<UUID, TrialBookingDetailsDto.Student> getParticipantDetails(
            Collection<UUID> participantIds
    );
}
