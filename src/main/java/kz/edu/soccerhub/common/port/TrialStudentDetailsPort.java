package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;

import java.util.UUID;

public interface TrialStudentDetailsPort {

    /**
     * Retrieves the details of a trial student by their unique identifier.
     *
     * @param studentId the unique identifier of the trial student
     * @return a TrialBookingDetailsDto.Student containing the details of the trial student
     */
    TrialBookingDetailsDto.Student getDetails(UUID studentId);
}