package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.trial.TrialSessionContext;

import java.util.Collection;
import java.util.Map;
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

    /**
     * Retrieves details of multiple trial sessions by their IDs.
     *
     * @param sessionIds a collection of unique identifiers of the trial sessions
     * @return a map where the key is the session ID and the value is the corresponding TrialSessionContext
     */
    Map<UUID, TrialSessionContext> getSessionDetails(Collection<UUID> sessionIds);
}