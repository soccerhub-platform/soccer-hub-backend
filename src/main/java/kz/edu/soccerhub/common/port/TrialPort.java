package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.trial.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface TrialPort {

    /**
     * Creates a new trial booking based on the provided command.
     * @param command the command containing the details for creating the trial booking
     * @return the DTO representing the created trial booking
     * @throws IllegalArgumentException if any validation fails during the creation process
     */
    TrialBookingDto createTrial(
            CreateTrialBookingCommand command
    );

    Page<TrialBookingDto> findTrials(
            TrialBookingSearchCommand command,
            Pageable pageable
    );

    TrialBookingDto getTrial(UUID trialId);

    TrialBookingDetailsDto getTrialDetails(UUID trialId);

    TrialBookingDetailsDto confirmTrial(UUID trialId, UUID adminId);

    TrialBookingDetailsDto cancelTrial(CancelTrialCommand command);

    TrialBookingDetailsDto markAttendance(MarkTrialAttendanceCommand command);

    TrialBookingDetailsDto recordResult(RecordTrialResultCommand command);

    void linkConvertedStudent(LinkTrialStudentCommand command);

    Page<TrialBookingListItemDto> findList(
            TrialBookingSearchCommand command,
            Pageable pageable
    );
}
