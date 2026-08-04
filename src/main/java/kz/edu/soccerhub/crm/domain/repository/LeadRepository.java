package kz.edu.soccerhub.crm.domain.repository;

import kz.edu.soccerhub.crm.domain.model.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID>, JpaSpecificationExecutor<Lead> {

    @Query("""
        select (count(l) > 0)
        from Lead l
        join l.participants participant
        where l.primaryContactPhone = :phone
          and lower(trim(participant.fullName)) = :participantName
          and (
              (:birthDate is null and participant.birthDate is null)
              or participant.birthDate = :birthDate
          )
          and l.status in (
              kz.edu.soccerhub.crm.domain.model.enums.LeadStatus.NEW,
              kz.edu.soccerhub.crm.domain.model.enums.LeadStatus.IN_PROGRESS,
              kz.edu.soccerhub.crm.domain.model.enums.LeadStatus.TRIAL_SCHEDULED,
              kz.edu.soccerhub.crm.domain.model.enums.LeadStatus.DECISION_PENDING
          )
        """)
    boolean existsActiveLead(
            String phone,
            String participantName,
            LocalDate birthDate
    );

    List<Lead> findByParticipantIdInOrderByUpdatedAtDesc(Collection<UUID> participantIds);

}
