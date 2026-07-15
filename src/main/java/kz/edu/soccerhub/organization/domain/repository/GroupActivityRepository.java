package kz.edu.soccerhub.organization.domain.repository;

import kz.edu.soccerhub.organization.domain.model.GroupActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GroupActivityRepository extends JpaRepository<GroupActivity, UUID> {

    Page<GroupActivity> findByGroupIdOrderByOccurredAtDesc(UUID groupId, Pageable pageable);
}
