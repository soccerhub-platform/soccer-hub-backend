package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.trial.TrialSessionContext;

import java.util.UUID;

public interface TrialSessionPort {
    /**
     * Retrieves a bookable trial session by its ID.
     *
     * @param sessionId the unique identifier of the trial session
     * @return a TrialSessionContext containing details of the bookable session
     */
    TrialSessionContext getBookableSession(UUID sessionId);

    /**
     * Retrieves details of a trial session by its ID.
     *
     * @param sessionId the unique identifier of the trial session
     * @return a TrialSessionContext containing details of the trial session
     */
    TrialSessionContext getSessionDetails(UUID sessionId);
}