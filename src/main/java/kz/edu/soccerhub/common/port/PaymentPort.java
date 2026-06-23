package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.payment.ContractPaymentSummaryOutput;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentSummaryQueryInput;
import kz.edu.soccerhub.common.dto.payment.PaymentCancelCommand;
import kz.edu.soccerhub.common.dto.payment.PaymentCreateCommand;
import kz.edu.soccerhub.common.dto.payment.PaymentCreateOutput;
import kz.edu.soccerhub.common.dto.payment.PaymentOutput;
import kz.edu.soccerhub.common.dto.payment.PaymentSearchQuery;
import kz.edu.soccerhub.common.dto.payment.PaymentsPageOutput;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PaymentPort {

    PaymentCreateOutput createPayment(PaymentCreateCommand command, UUID actorUserId);

    PaymentsPageOutput listPayments(PaymentSearchQuery query, Pageable pageable);

    PaymentOutput getPayment(UUID paymentId);

    PaymentOutput cancelPayment(UUID paymentId, PaymentCancelCommand command, UUID actorUserId);

    ContractPaymentSummaryOutput getContractPaymentSummary(UUID contractId);

    Map<UUID, ContractPaymentSummaryOutput> getContractPaymentSummaries(Collection<ContractPaymentSummaryQueryInput> contracts);

    List<PaymentOutput> getContractPayments(UUID contractId);
}
