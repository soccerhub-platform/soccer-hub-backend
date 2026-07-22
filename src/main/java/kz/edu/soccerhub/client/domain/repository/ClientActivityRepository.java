package kz.edu.soccerhub.client.domain.repository;

import kz.edu.soccerhub.client.domain.model.ClientActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClientActivityRepository extends JpaRepository<ClientActivity, UUID> {
    Page<ClientActivity> findByClientIdOrderByOccurredAtDesc(UUID clientId, Pageable pageable);
}
