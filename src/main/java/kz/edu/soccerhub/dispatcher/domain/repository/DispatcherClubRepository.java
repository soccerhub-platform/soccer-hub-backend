package kz.edu.soccerhub.dispatcher.domain.repository;

import kz.edu.soccerhub.dispatcher.domain.model.DispatcherClub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.UUID;

@Repository
public interface DispatcherClubRepository extends JpaRepository<DispatcherClub, DispatcherClub.DispatcherClubId> {
    boolean existsById(DispatcherClub.DispatcherClubId dispatcherClubId);
    Collection<DispatcherClub> findAllById(DispatcherClub.DispatcherClubId id);
    Collection<DispatcherClub> findByIdDispatcherId(UUID dispatcherId);
}