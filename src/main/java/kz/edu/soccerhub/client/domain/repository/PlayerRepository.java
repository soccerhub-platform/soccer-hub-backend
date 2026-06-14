package kz.edu.soccerhub.client.domain.repository;

import kz.edu.soccerhub.client.domain.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
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

    List<Player> findByIdIn(Iterable<UUID> ids);

    @Query("""
            select p
            from Player p
            join fetch p.parent parent
            where parent.branchId = :branchId
            order by lower(p.firstName), lower(p.lastName)
            """)
    List<Player> findAllByParentBranchId(UUID branchId);
}
