package kz.edu.soccerhub.payments.application;

import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentContextOutput;
import kz.edu.soccerhub.common.dto.client.ClientActivityType;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentStatus;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentSummaryOutput;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentSummaryQueryInput;
import kz.edu.soccerhub.common.dto.payment.PaymentCancelCommand;
import kz.edu.soccerhub.common.dto.payment.PaymentCreateCommand;
import kz.edu.soccerhub.common.dto.payment.PaymentCreateOutput;
import kz.edu.soccerhub.common.dto.payment.PaymentOutput;
import kz.edu.soccerhub.common.port.AdminPort;
import kz.edu.soccerhub.common.port.ClientActivityPort;
import kz.edu.soccerhub.common.port.ContractPort;
import kz.edu.soccerhub.payments.domain.enums.PaymentMethod;
import kz.edu.soccerhub.payments.domain.enums.PaymentStatus;
import kz.edu.soccerhub.payments.domain.model.Payment;
import kz.edu.soccerhub.payments.domain.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ContractPort contractPort;
    @Mock
    private AdminPort adminPort;
    @Mock
    private ClientActivityPort clientActivityPort;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                paymentRepository,
                contractPort,
                adminPort,
                clientActivityPort,
                new ContractPaymentCalculator()
        );
    }

    @Test
    void createPaymentShouldSyncLeadWhenContractBecomesFullyPaid() {
        UUID contractId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        ContractPaymentContextOutput context = context(contractId, BigDecimal.valueOf(80000));
        PaymentCreateCommand command = command(contractId, BigDecimal.valueOf(80000));

        when(contractPort.getPaymentContext(contractId)).thenReturn(context);
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.findByContractIdOrderByPaidAtDescCreatedAtDesc(contractId))
                .thenReturn(List.of(savedPayment(contractId, context, command.amount())));

        PaymentCreateOutput output = paymentService.createPayment(command, actorId);

        assertEquals(PaymentStatus.PAID, output.paymentStatus());
        assertEquals(BigDecimal.ZERO, output.outstandingAmount());
        verify(clientActivityPort).recordClientActivity(
                org.mockito.ArgumentMatchers.eq(context.clientId()),
                org.mockito.ArgumentMatchers.eq(actorId),
                org.mockito.ArgumentMatchers.eq(ClientActivityType.PAYMENT_CREATED),
                any()
        );
    }

    @Test
    void createPaymentShouldNotSyncLeadWhenContractStillPartiallyPaid() {
        UUID contractId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        ContractPaymentContextOutput context = context(contractId, BigDecimal.valueOf(80000));
        PaymentCreateCommand command = command(contractId, BigDecimal.valueOf(50000));

        when(contractPort.getPaymentContext(contractId)).thenReturn(context);
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.findByContractIdOrderByPaidAtDescCreatedAtDesc(contractId))
                .thenReturn(List.of(savedPayment(contractId, context, command.amount())));

        PaymentCreateOutput output = paymentService.createPayment(command, actorId);

        assertEquals(BigDecimal.valueOf(30000), output.outstandingAmount());
    }

    @Test
    void batchSummaryShouldReturnZeroStateForMissingPaymentsAndUseSingleRepositoryCall() {
        UUID firstContractId = UUID.randomUUID();
        UUID secondContractId = UUID.randomUUID();

        when(paymentRepository.findByContractIdInOrderByPaidAtDescCreatedAtDesc(argThat(ids ->
                ids.contains(firstContractId) && ids.contains(secondContractId) && ids.size() == 2
        ))).thenReturn(List.of(
                savedPayment(firstContractId, context(firstContractId, BigDecimal.valueOf(80000)), BigDecimal.valueOf(50000)),
                cancelledPayment(firstContractId, context(firstContractId, BigDecimal.valueOf(80000)), BigDecimal.valueOf(10000))
        ));

        Map<UUID, ContractPaymentSummaryOutput> summaries = paymentService.getContractPaymentSummaries(List.of(
                new ContractPaymentSummaryQueryInput(firstContractId, BigDecimal.valueOf(80000)),
                new ContractPaymentSummaryQueryInput(secondContractId, BigDecimal.valueOf(30000))
        ));

        assertEquals(ContractPaymentStatus.PARTIALLY_PAID, summaries.get(firstContractId).paymentStatus());
        assertEquals(BigDecimal.valueOf(50000), summaries.get(firstContractId).paidAmount());
        assertEquals(ContractPaymentStatus.UNPAID, summaries.get(secondContractId).paymentStatus());
        assertEquals(BigDecimal.ZERO, summaries.get(secondContractId).paidAmount());
        assertEquals(BigDecimal.valueOf(30000), summaries.get(secondContractId).outstandingAmount());
        verify(paymentRepository).findByContractIdInOrderByPaidAtDescCreatedAtDesc(argThat(ids ->
                ids.contains(firstContractId) && ids.contains(secondContractId) && ids.size() == 2
        ));
    }

    @Test
    void cancelPaymentShouldOnlyCancelPayment() {
        UUID contractId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        ContractPaymentContextOutput context = context(contractId, BigDecimal.valueOf(80000));
        Payment payment = savedPayment(contractId, context, BigDecimal.valueOf(80000));

        when(paymentRepository.findById(payment.getId())).thenReturn(java.util.Optional.of(payment));
        when(contractPort.getPaymentContext(contractId)).thenReturn(context);

        PaymentOutput output = paymentService.cancelPayment(
                payment.getId(),
                new PaymentCancelCommand("duplicate payment", "reverted manually"),
                actorId
        );

        assertEquals(PaymentStatus.CANCELLED, output.status());
        verify(clientActivityPort).recordClientActivity(
                org.mockito.ArgumentMatchers.eq(context.clientId()),
                org.mockito.ArgumentMatchers.eq(actorId),
                org.mockito.ArgumentMatchers.eq(ClientActivityType.PAYMENT_CANCELLED),
                any()
        );
    }

    private ContractPaymentContextOutput context(UUID contractId, BigDecimal amount) {
        return new ContractPaymentContextOutput(
                contractId,
                "CNT-2026-00001",
                UUID.randomUUID(),
                "Jane Doe",
                UUID.randomUUID(),
                "Alex Doe",
                UUID.randomUUID(),
                amount,
                "KZT",
                ContractStatus.ACTIVE
        );
    }

    private PaymentCreateCommand command(UUID contractId, BigDecimal amount) {
        return new PaymentCreateCommand(
                contractId,
                amount,
                "KZT",
                PaymentMethod.KASPI,
                LocalDateTime.of(2026, 6, 23, 12, 0),
                "July",
                "ref-1"
        );
    }

    private Payment savedPayment(UUID contractId, ContractPaymentContextOutput context, BigDecimal amount) {
        return Payment.builder()
                .id(UUID.randomUUID())
                .contractId(contractId)
                .clientId(context.clientId())
                .playerId(context.playerId())
                .branchId(context.branchId())
                .amount(amount)
                .currency("KZT")
                .status(PaymentStatus.PAID)
                .method(PaymentMethod.KASPI)
                .paidAt(LocalDateTime.of(2026, 6, 23, 12, 0))
                .recordedAt(LocalDateTime.of(2026, 6, 23, 12, 0))
                .recordedBy(UUID.randomUUID())
                .build();
    }

    private Payment cancelledPayment(UUID contractId, ContractPaymentContextOutput context, BigDecimal amount) {
        return Payment.builder()
                .id(UUID.randomUUID())
                .contractId(contractId)
                .clientId(context.clientId())
                .playerId(context.playerId())
                .branchId(context.branchId())
                .amount(amount)
                .currency("KZT")
                .status(PaymentStatus.CANCELLED)
                .method(PaymentMethod.KASPI)
                .paidAt(LocalDateTime.of(2026, 6, 24, 12, 0))
                .recordedAt(LocalDateTime.of(2026, 6, 24, 12, 0))
                .recordedBy(UUID.randomUUID())
                .build();
    }
}
