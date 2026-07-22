package kz.edu.soccerhub.payments.domain.repository;

import kz.edu.soccerhub.payments.domain.model.Payment;
import kz.edu.soccerhub.payments.domain.enums.PaymentMethod;
import kz.edu.soccerhub.payments.domain.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    @Query("""
            select p
            from Payment p
            join kz.edu.soccerhub.client.domain.model.Contract c on c.id = p.contractId
            left join kz.edu.soccerhub.client.domain.model.Player pl on pl.id = p.playerId
            left join kz.edu.soccerhub.client.domain.model.Client cl on cl.id = p.clientId
            where p.branchId = :branchId
              and (cast(:contractId as uuid) is null or p.contractId = :contractId)
              and (cast(:clientId as uuid) is null or p.clientId = :clientId)
              and (
                    :searchText is null
                    or lower(c.contractNumber) like :searchText
                    or lower(trim(concat(coalesce(cl.firstName, ''), concat(' ', coalesce(cl.lastName, ''))))) like :searchText
                    or lower(trim(concat(coalesce(pl.firstName, ''), concat(' ', coalesce(pl.lastName, ''))))) like :searchText
                    or lower(coalesce(p.externalReference, '')) like :searchText
                    or lower(coalesce(p.comment, '')) like :searchText
                    or (:searchId is not null and (
                        p.id = :searchId
                        or p.contractId = :searchId
                        or p.clientId = :searchId
                        or p.playerId = :searchId
                    ))
              )
              and (:statusesEmpty = true or p.status in :statuses)
              and (:methodsEmpty = true or p.method in :methods)
              and (cast(:paidFrom as timestamp) is null or p.paidAt >= :paidFrom)
              and (cast(:paidTo as timestamp) is null or p.paidAt <= :paidTo)
            """)
    Page<Payment> search(
            UUID branchId,
            UUID contractId,
            UUID clientId,
            String searchText,
            UUID searchId,
            Collection<PaymentStatus> statuses,
            boolean statusesEmpty,
            Collection<PaymentMethod> methods,
            boolean methodsEmpty,
            LocalDateTime paidFrom,
            LocalDateTime paidTo,
            Pageable pageable
    );

    List<Payment> findByContractIdOrderByPaidAtDescCreatedAtDesc(UUID contractId);

    List<Payment> findByContractIdInOrderByPaidAtDescCreatedAtDesc(Collection<UUID> contractIds);

    Optional<Payment> findTopByContractIdAndStatusOrderByPaidAtDescCreatedAtDesc(UUID contractId, PaymentStatus status);
}
