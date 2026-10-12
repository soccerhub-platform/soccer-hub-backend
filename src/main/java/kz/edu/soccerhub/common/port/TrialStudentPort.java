package kz.edu.soccerhub.common.port;

import java.util.UUID;

public interface TrialStudentPort {
    void validateBranch(UUID studentId,UUID branchId);
    /**
     * Validates if the trial student with the given ID exists.
     *
     * @param studentId the ID of the trial student to validate
     * @throws IllegalArgumentException if the student does not exist
     */
    void validateExists(UUID studentId);
}