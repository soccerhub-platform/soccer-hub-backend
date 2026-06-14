package kz.edu.soccerhub.client.domain.repository;

import kz.edu.soccerhub.client.domain.model.Contract;
import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.crm.domain.model.enums.LeadType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
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

    Optional<Contract> findTopByPlayerIdOrderByCreatedAtDesc(UUID playerId);

    boolean existsByContractNumber(String contractNumber);

    @Query("""
            select c
            from Contract c
            join kz.edu.soccerhub.client.domain.model.Player p on p.id = c.playerId
            join p.parent cl
            join kz.edu.soccerhub.organization.domain.model.Group g on g.id = c.groupId
            where cl.branchId = :branchId
              and (:leadType is null or c.leadType = :leadType)
              and (:statusesEmpty = true or c.status in :statuses)
              and (
                    :searchText is null
                    or lower(c.contractNumber) like :searchText
                    or lower(trim(concat(coalesce(p.firstName, ''), concat(' ', coalesce(p.lastName, ''))))) like :searchText
                    or lower(trim(concat(coalesce(cl.firstName, ''), concat(' ', coalesce(cl.lastName, ''))))) like :searchText
                    or lower(coalesce(cl.phone, '')) like :searchText
                    or lower(coalesce(g.name, '')) like :searchText
                    or (:searchId is not null and c.id = :searchId)
              )
            """)
    Page<Contract> search(
            UUID branchId,
            LeadType leadType,
            Collection<ContractStatus> statuses,
            boolean statusesEmpty,
            String searchText,
            UUID searchId,
            Pageable pageable
    );

    @Query("""
            select (count(c) > 0)
            from Contract c
            where c.playerId = :playerId
              and c.status <> kz.edu.soccerhub.client.domain.enums.ContractStatus.CANCELLED
              and (:excludedContractId is null or c.id <> :excludedContractId)
              and c.startDate <= :endDate
              and (c.endDate is null or c.endDate >= :startDate)
            """)
    boolean existsOverlappingContractInRange(
            UUID playerId,
            LocalDate startDate,
            LocalDate endDate,
            UUID excludedContractId
    );

    @Query("""
            select (count(c) > 0)
            from Contract c
            where c.playerId = :playerId
              and c.status <> kz.edu.soccerhub.client.domain.enums.ContractStatus.CANCELLED
              and (:excludedContractId is null or c.id <> :excludedContractId)
              and (c.endDate is null or c.endDate >= :startDate)
            """)
    boolean existsOpenEndedOverlap(
            UUID playerId,
            LocalDate startDate,
            UUID excludedContractId
    );
}
