package kz.edu.soccerhub.crm.domain.repository;

import kz.edu.soccerhub.crm.domain.model.LeadTrial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LeadTrialRepository extends JpaRepository<LeadTrial, UUID> {
}

