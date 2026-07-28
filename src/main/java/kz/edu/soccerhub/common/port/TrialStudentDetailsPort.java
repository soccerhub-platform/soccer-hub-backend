package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface TrialStudentDetailsPort {

    /**
     * Retrieves the details of a trial student by their unique identifier.
     *
     * @param studentId the unique identifier of the trial student
     * @return a TrialBookingDetailsDto.Student containing the details of the trial student
     */
    TrialBookingDetailsDto.Student getDetails(UUID studentId);

    /**
     * Retrieves the details of multiple trial students by their unique identifiers.
     *
     * @param studentIds a collection of unique identifiers of the trial students
     * @return a map where the key is the student ID and the value is the corresponding TrialBookingDetailsDto.Student
     */
    Map<UUID, TrialBookingDetailsDto.Student> getDetails(
            Collection<UUID> studentIds
    );
}