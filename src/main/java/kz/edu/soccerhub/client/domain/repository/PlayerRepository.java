package kz.edu.soccerhub.client.domain.repository;

import kz.edu.soccerhub.client.domain.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PlayerRepository extends JpaRepository<Player, UUID> {

    Optional<Player> findFirstByParent_IdAndFirstNameAndLastNameAndBirthDate(
            UUID parentId,
            String firstName,
            String lastName,
            LocalDate birthDate
    );
}
