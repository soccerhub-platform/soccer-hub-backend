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
            where p.branchId = :branchId
              and (:contractId is null or p.contractId = :contractId)
              and (:clientId is null or p.clientId = :clientId)
              and (:statusesEmpty = true or p.status in :statuses)
              and (:methodsEmpty = true or p.method in :methods)
              and (:paidFrom is null or p.paidAt >= :paidFrom)
              and (:paidTo is null or p.paidAt <= :paidTo)
            """)
    Page<Payment> search(
            UUID branchId,
            UUID contractId,
            UUID clientId,
            Collection<PaymentStatus> statuses,
            boolean statusesEmpty,
            Collection<PaymentMethod> methods,
            boolean methodsEmpty,
            LocalDateTime paidFrom,
            LocalDateTime paidTo,
            Pageable pageable
    );

    List<Payment> findByContractIdOrderByPaidAtDescCreatedAtDesc(UUID contractId);

    Optional<Payment> findTopByContractIdAndStatusOrderByPaidAtDescCreatedAtDesc(UUID contractId, PaymentStatus status);
}
