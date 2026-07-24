package kz.edu.soccerhub.crm.domain.repository;

import kz.edu.soccerhub.crm.domain.model.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeadRepository extends JpaRepository<Lead, UUID>, JpaSpecificationExecutor<Lead> {

    @Query("""
            select (count(l) > 0)
            from Lead l
            where l.primaryContactPhone = :phone
              and l.status in (
                  kz.edu.soccerhub.crm.domain.model.enums.LeadStatus.NEW,
                  kz.edu.soccerhub.crm.domain.model.enums.LeadStatus.IN_PROGRESS,
                  kz.edu.soccerhub.crm.domain.model.enums.LeadStatus.TRIAL_SCHEDULED,
                  kz.edu.soccerhub.crm.domain.model.enums.LeadStatus.DECISION_PENDING
              )
            """)
    boolean existsActiveLeadByPhone(String phone);

    List<Lead> findByParticipantIdInOrderByUpdatedAtDesc(Collection<UUID> participantIds);

}
