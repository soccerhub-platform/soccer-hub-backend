package kz.edu.soccerhub.dispatcher.domain.repository;

import kz.edu.soccerhub.dispatcher.domain.model.DispatcherProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DispatcherProfileRepository extends JpaRepository<DispatcherProfile, UUID> {
}
