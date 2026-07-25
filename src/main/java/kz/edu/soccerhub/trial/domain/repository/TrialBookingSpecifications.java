package kz.edu.soccerhub.trial.domain.repository;

import kz.edu.soccerhub.common.dto.trial.TrialBookingSearchCommand;
import kz.edu.soccerhub.trial.domain.entity.TrialBooking;
import org.springframework.data.jpa.domain.Specification;

public final class TrialBookingSpecifications {

    private TrialBookingSpecifications() {
    }

    public static Specification<TrialBooking> byQuery(
            TrialBookingSearchCommand command
    ) {
        Specification<TrialBooking> specification = (root, query, builder) ->
                builder.conjunction();

        if (command.status() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("status"), command.status()));
        }
        if (command.leadId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("leadId"), command.leadId()));
        }
        if (command.clientId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("clientId"), command.clientId()));
        }
        if (command.studentId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("studentId"), command.studentId()));
        }
        if (command.trainingSessionId() != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("trainingSessionId"), command.trainingSessionId()));
        }

        return specification;
    }
}
