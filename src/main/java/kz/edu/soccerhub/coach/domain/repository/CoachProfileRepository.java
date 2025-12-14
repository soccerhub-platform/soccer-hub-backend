package kz.edu.soccerhub.coach.domain.repository;

import kz.edu.soccerhub.coach.domain.model.CoachProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Set;
import java.util.UUID;

@Repository
public interface CoachProfileRepository extends JpaRepository<CoachProfile, UUID> {

    @Query("""
        select cp
        from CoachProfile cp
        where exists (
            select 1
            from CoachBranch cb
            where cb.coachId = cp.id
              and cb.branchId in :branchIds
        )
    """)
    Page<CoachProfile> findAccessibleCoaches(
            @Param("branchIds") Set<UUID> branchIds,
            Pageable pageable
    );
}
