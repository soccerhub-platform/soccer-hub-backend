package kz.edu.soccerhub.payments.application;

import kz.edu.soccerhub.common.dto.payment.ContractPaymentSummaryOutput;
import kz.edu.soccerhub.common.dto.payment.PaymentCancelCommand;
import kz.edu.soccerhub.common.dto.payment.PaymentCreateCommand;
import kz.edu.soccerhub.common.dto.payment.PaymentCreateOutput;
import kz.edu.soccerhub.common.dto.payment.PaymentOutput;
import kz.edu.soccerhub.common.dto.payment.PaymentSearchQuery;
import kz.edu.soccerhub.common.dto.payment.PaymentsPageOutput;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.AdminBranchAccessPort;
import kz.edu.soccerhub.common.port.AdminPort;
import kz.edu.soccerhub.common.port.ContractPort;
import kz.edu.soccerhub.common.port.PaymentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminPaymentService {

    private final PaymentPort paymentPort;
    private final ContractPort contractPort;
    private final AdminPort adminPort;
    private final AdminBranchAccessPort adminBranchAccessPort;

    @Transactional
    public PaymentCreateOutput create(UUID adminId, PaymentCreateCommand command) {
        verifyAdminExists(adminId);
        verifyAdminBranchAccess(adminId, contractPort.getBranchId(command.contractId()));
        return paymentPort.createPayment(command, adminId);
    }

    @Transactional(readOnly = true)
    public PaymentsPageOutput list(UUID adminId, PaymentSearchQuery query, Pageable pageable) {
        verifyAdminExists(adminId);
        verifyAdminBranchAccess(adminId, query.branchId());
        return paymentPort.listPayments(query, pageable);
    }

    @Transactional(readOnly = true)
    public PaymentOutput get(UUID adminId, UUID paymentId) {
        verifyAdminExists(adminId);
        PaymentOutput payment = paymentPort.getPayment(paymentId);
        verifyAdminBranchAccess(adminId, payment.branchId());
        return payment;
    }

    @Transactional
    public PaymentOutput cancel(UUID adminId, UUID paymentId, PaymentCancelCommand command) {
        verifyAdminExists(adminId);
        PaymentOutput existing = paymentPort.getPayment(paymentId);
        verifyAdminBranchAccess(adminId, existing.branchId());
        return paymentPort.cancelPayment(paymentId, command, adminId);
    }

    @Transactional(readOnly = true)
    public ContractPaymentSummaryOutput getContractSummary(UUID adminId, UUID contractId) {
        verifyAdminExists(adminId);
        verifyAdminBranchAccess(adminId, contractPort.getBranchId(contractId));
        return paymentPort.getContractPaymentSummary(contractId);
    }

    @Transactional(readOnly = true)
    public List<PaymentOutput> getContractPayments(UUID adminId, UUID contractId) {
        verifyAdminExists(adminId);
        verifyAdminBranchAccess(adminId, contractPort.getBranchId(contractId));
        return paymentPort.getContractPayments(contractId);
    }

    private void verifyAdminExists(UUID adminId) {
        if (!adminPort.verifyAdmin(adminId)) {
            throw new NotFoundException("Admin not found", adminId);
        }
    }

    private void verifyAdminBranchAccess(UUID adminId, UUID branchId) {
        if (!adminBranchAccessPort.verifyAdminBelongsToBranch(adminId, branchId)) {
            throw new BadRequestException("Admin does not have access to branch", branchId);
        }
    }
}
