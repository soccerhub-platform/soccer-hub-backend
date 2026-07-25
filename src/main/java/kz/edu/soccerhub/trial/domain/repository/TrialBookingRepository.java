package kz.edu.soccerhub.trial.domain.repository;

import kz.edu.soccerhub.trial.domain.entity.TrialBooking;
import kz.edu.soccerhub.trial.domain.enums.TrialBookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface TrialBookingRepository extends JpaRepository<TrialBooking, UUID>, JpaSpecificationExecutor<TrialBooking> {

    boolean existsByStudentIdAndTrainingSessionIdAndStatusIn(
            UUID studentId,
            UUID trainingSessionId,
            Collection<TrialBookingStatus> statuses
    );

    List<TrialBooking> findAllByStatusOrderByCreatedAtDesc(TrialBookingStatus status);

    List<TrialBooking> findAllByLeadIdAndParticipantIdAndStudentIdIsNull(UUID leadId, UUID participantId);
}
