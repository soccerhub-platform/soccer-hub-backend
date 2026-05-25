package kz.edu.soccerhub.client.domain.repository;

import kz.edu.soccerhub.client.domain.model.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID> {

    Optional<Contract> findFirstByPlayerIdAndGroupIdAndStartDateAndEndDate(
            UUID playerId,
            UUID groupId,
            LocalDate startDate,
            LocalDate endDate
    );

    List<Contract> findByGroupId(UUID groupId);
}
