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
          and participant.birthDate = :birthDate
          and l.status in (
              kz.edu.soccerhub.crm.domain.model.enums.LeadStatus.NEW,
              kz.edu.soccerhub.crm.domain.model.enums.LeadStatus.IN_PROGRESS,
              kz.edu.soccerhub.crm.domain.model.enums.LeadStatus.TRIAL_SCHEDULED,
              kz.edu.soccerhub.crm.domain.model.enums.LeadStatus.DECISION_PENDING,
              kz.edu.soccerhub.crm.domain.model.enums.LeadStatus.CONTRACT_PENDING,
              kz.edu.soccerhub.crm.domain.model.enums.LeadStatus.PAYMENT_PENDING
          )
        """)
    boolean existsActiveLeadWithBirthDate(
            String phone,
            String participantName,
            LocalDate birthDate
    );

    @Query("""
        select (count(l) > 0)
        from Lead l
        join l.participants participant
        where l.primaryContactPhone = :phone
          and lower(trim(participant.fullName)) = :participantName
          and participant.birthDate is null
          and l.status in (
              kz.edu.soccerhub.crm.domain.model.enums.LeadStatus.NEW,
              kz.edu.soccerhub.crm.domain.model.enums.LeadStatus.IN_PROGRESS,
              kz.edu.soccerhub.crm.domain.model.enums.LeadStatus.TRIAL_SCHEDULED,
              kz.edu.soccerhub.crm.domain.model.enums.LeadStatus.DECISION_PENDING,
              kz.edu.soccerhub.crm.domain.model.enums.LeadStatus.CONTRACT_PENDING,
              kz.edu.soccerhub.crm.domain.model.enums.LeadStatus.PAYMENT_PENDING
          )
        """)
    boolean existsActiveLeadWithoutBirthDate(
            String phone,
            String participantName
    );

    @Query("""
        select distinct l
        from Lead l
        join l.participants participant
        where participant.id in :participantIds
        order by l.updatedAt desc
        """)
    List<Lead> findByLeadParticipantIdInOrderByUpdatedAtDesc(Collection<UUID> participantIds);

}
