package kz.edu.soccerhub.crm.domain.repository;

import kz.edu.soccerhub.crm.domain.model.LeadLossReasonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeadLossReasonRepository extends JpaRepository<LeadLossReasonEntity, String> {

    Optional<LeadLossReasonEntity> findByCodeAndActiveTrue(String code);

    List<LeadLossReasonEntity> findByActiveTrueOrderBySortOrderAscNameAsc();
}
