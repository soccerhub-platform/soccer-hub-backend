package kz.edu.soccerhub.crm.domain.repository;

import kz.edu.soccerhub.crm.domain.model.LeadParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LeadParticipantRepository extends JpaRepository<LeadParticipant, UUID> {
}
