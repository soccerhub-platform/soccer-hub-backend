package kz.edu.soccerhub.payments.application;

import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.dto.client.ClientActivityType;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentContextOutput;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentSummaryOutput;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentSummaryQueryInput;
import kz.edu.soccerhub.common.dto.payment.PaymentCancelCommand;
import kz.edu.soccerhub.common.dto.payment.PaymentCreateCommand;
import kz.edu.soccerhub.common.dto.payment.PaymentCreateOutput;
import kz.edu.soccerhub.common.dto.payment.PaymentOutput;
import kz.edu.soccerhub.common.dto.payment.PaymentSearchQuery;
import kz.edu.soccerhub.common.dto.payment.PaymentsPageOutput;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.AdminPort;
import kz.edu.soccerhub.common.port.ClientActivityPort;
import kz.edu.soccerhub.common.port.ContractPort;
import kz.edu.soccerhub.common.port.PaymentPort;
import kz.edu.soccerhub.payments.domain.enums.PaymentStatus;
import kz.edu.soccerhub.payments.domain.model.Payment;
import kz.edu.soccerhub.payments.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService implements PaymentPort {

    private final PaymentRepository paymentRepository;
    private final ContractPort contractPort;
    private final AdminPort adminPort;
    private final ClientActivityPort clientActivityPort;
    private final ContractPaymentCalculator contractPaymentCalculator;

    @Override
    @Transactional
    public PaymentCreateOutput createPayment(PaymentCreateCommand command, UUID actorUserId) {
        ContractPaymentContextOutput contract = contractPort.getPaymentContext(command.contractId());
        validateCreateCommand(command, contract);

        Payment payment = paymentRepository.save(Payment.builder()
                .id(UUID.randomUUID())
                .contractId(contract.contractId())
                .clientId(contract.clientId())
                .playerId(contract.playerId())
                .branchId(contract.branchId())
                .amount(command.amount())
                .currency(contract.currency())
                .status(PaymentStatus.PAID)
                .method(command.method())
                .paidAt(command.paidAt())
                .recordedAt(LocalDateTime.now())
                .recordedBy(actorUserId)
                .comment(trimToNull(command.comment()))
                .externalReference(trimToNull(command.externalReference()))
                .build());

        ContractPaymentSummaryOutput summary = getContractPaymentSummary(contract.contractId());
        recordPaymentActivity(payment, contract, actorUserId, ClientActivityType.PAYMENT_CREATED);

        return new PaymentCreateOutput(
                payment.getId(),
                payment.getContractId(),
                payment.getStatus(),
                summary.paymentStatus(),
                summary.paidAmount(),
                summary.outstandingAmount(),
                summary.overpaidAmount()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentsPageOutput listPayments(PaymentSearchQuery query, Pageable pageable) {
        Page<Payment> page = paymentRepository.search(
                query.branchId(),
                query.contractId(),
                query.clientId(),
                normalizeSearchLike(query.search()),
                parseUuid(query.search()),
                query.statuses() == null ? List.of() : query.statuses(),
                query.statuses() == null || query.statuses().isEmpty(),
                query.methods() == null ? List.of() : query.methods(),
                query.methods() == null || query.methods().isEmpty(),
                query.paidFrom(),
                query.paidTo(),
                pageable
        );

        Map<UUID, ContractPaymentContextOutput> contractContexts = loadContractContexts(page.getContent());
        Map<UUID, String> actorNames = loadActorNames(page.getContent());

        return new PaymentsPageOutput(
                page.getContent().stream()
                        .map(payment -> toOutput(payment, contractContexts.get(payment.getContractId()), actorNames))
                        .toList(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize()
        );
    }

    private String normalizeSearchLike(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isEmpty()) {
            return null;
        }
        return "%" + trimmed + "%";
    }

    private UUID parseUuid(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(trimmed);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentOutput getPayment(UUID paymentId) {
        Payment payment = findPayment(paymentId);
        ContractPaymentContextOutput contractContext = contractPort.getPaymentContext(payment.getContractId());
        String actorName = resolveAdminName(payment.getRecordedBy());
        return toOutput(payment, contractContext, Map.of(payment.getRecordedBy(), actorName));
    }

    @Override
    @Transactional
    public PaymentOutput cancelPayment(UUID paymentId, PaymentCancelCommand command, UUID actorUserId) {
        Payment payment = findPayment(paymentId);
        if (payment.isCancelled()) {
            throw new BadRequestException("Payment is already cancelled", paymentId);
        }

        payment.cancel(trimToNull(command.reason()), trimToNull(command.comment()));
        payment.setRecordedBy(actorUserId);
        payment.setRecordedAt(LocalDateTime.now());

        ContractPaymentContextOutput contractContext = contractPort.getPaymentContext(payment.getContractId());
        recordPaymentActivity(payment, contractContext, actorUserId, ClientActivityType.PAYMENT_CANCELLED);
        return toOutput(payment, contractContext, Map.of(actorUserId, resolveAdminName(actorUserId)));
    }

    @Override
    @Transactional(readOnly = true)
    public ContractPaymentSummaryOutput getContractPaymentSummary(UUID contractId) {
        ContractPaymentContextOutput contract = contractPort.getPaymentContext(contractId);
        return contractPaymentCalculator.summarize(
                contractId,
                contract.contractAmount(),
                paymentRepository.findByContractIdOrderByPaidAtDescCreatedAtDesc(contractId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, ContractPaymentSummaryOutput> getContractPaymentSummaries(Collection<ContractPaymentSummaryQueryInput> contracts) {
        if (contracts == null || contracts.isEmpty()) {
            return Map.of();
        }

        Map<UUID, ContractPaymentSummaryQueryInput> queryByContractId = contracts.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        ContractPaymentSummaryQueryInput::contractId,
                        item -> item,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        if (queryByContractId.isEmpty()) {
            return Map.of();
        }

        Map<UUID, List<Payment>> paymentsByContractId = paymentRepository
                .findByContractIdInOrderByPaidAtDescCreatedAtDesc(queryByContractId.keySet())
                .stream()
                .collect(Collectors.groupingBy(Payment::getContractId));

        Map<UUID, ContractPaymentSummaryOutput> result = new LinkedHashMap<>();
        for (ContractPaymentSummaryQueryInput contract : queryByContractId.values()) {
            result.put(
                    contract.contractId(),
                    contractPaymentCalculator.summarize(
                            contract.contractId(),
                            contract.contractAmount(),
                            paymentsByContractId.getOrDefault(contract.contractId(), List.of())
                    )
            );
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentOutput> getContractPayments(UUID contractId) {
        ContractPaymentContextOutput contractContext = contractPort.getPaymentContext(contractId);
        List<Payment> payments = paymentRepository.findByContractIdOrderByPaidAtDescCreatedAtDesc(contractId);
        Map<UUID, String> actorNames = loadActorNames(payments);
        return payments.stream()
                .map(payment -> toOutput(payment, contractContext, actorNames))
                .toList();
    }

    private void recordPaymentActivity(
            Payment payment,
            ContractPaymentContextOutput contract,
            UUID actorUserId,
            ClientActivityType activityType
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentId", payment.getId());
        payload.put("contractId", contract.contractId());
        payload.put("contractNumber", contract.contractNumber());
        payload.put("playerId", contract.playerId());
        payload.put("playerName", contract.playerName());
        payload.put("amount", payment.getAmount());
        payload.put("currency", payment.getCurrency());
        payload.put("method", payment.getMethod().name());
        payload.put("status", payment.getStatus().name());
        payload.put("paidAt", payment.getPaidAt().toString());
        if (payment.getCancelReason() != null) {
            payload.put("reason", payment.getCancelReason());
        }
        clientActivityPort.recordClientActivity(contract.clientId(), actorUserId, activityType, payload);
    }

    private void validateCreateCommand(PaymentCreateCommand command, ContractPaymentContextOutput contract) {
        if (contract.contractStatus() == kz.edu.soccerhub.client.domain.enums.ContractStatus.CANCELLED) {
            throw new BadRequestException("Cannot create payment for cancelled contract", command.contractId());
        }
        if (command.amount() == null || command.amount().signum() <= 0) {
            throw new BadRequestException("amount must be > 0", command.amount());
        }
        if (!Objects.equals(contract.currency(), command.currency())) {
            throw new BadRequestException(
                    "Payment currency must match contract currency",
                    Map.of("contractCurrency", contract.currency(), "paymentCurrency", command.currency())
            );
        }
    }

    private Payment findPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NotFoundException("Payment not found", paymentId));
    }

    private Map<UUID, ContractPaymentContextOutput> loadContractContexts(List<Payment> payments) {
        return payments.stream()
                .map(Payment::getContractId)
                .distinct()
                .collect(Collectors.toMap(contractId -> contractId, contractPort::getPaymentContext));
    }

    private Map<UUID, String> loadActorNames(List<Payment> payments) {
        return payments.stream()
                .map(Payment::getRecordedBy)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toMap(actorId -> actorId, this::resolveAdminName));
    }

    private String resolveAdminName(UUID adminId) {
        return adminPort.findById(adminId)
                .map(this::buildAdminName)
                .filter(name -> !name.isBlank())
                .orElse(adminId == null ? "Unknown" : adminId.toString());
    }

    private String buildAdminName(AdminDto admin) {
        String firstName = admin.firstName() == null ? "" : admin.firstName().trim();
        String lastName = admin.lastName() == null ? "" : admin.lastName().trim();
        return (firstName + " " + lastName).trim();
    }

    private PaymentOutput toOutput(
            Payment payment,
            ContractPaymentContextOutput contractContext,
            Map<UUID, String> actorNames
    ) {
        return new PaymentOutput(
                payment.getId(),
                payment.getContractId(),
                contractContext == null ? null : contractContext.contractNumber(),
                payment.getClientId(),
                contractContext == null ? null : contractContext.clientName(),
                payment.getPlayerId(),
                contractContext == null ? null : contractContext.playerName(),
                payment.getBranchId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getPaidAt(),
                payment.getRecordedAt(),
                payment.getRecordedBy(),
                actorNames.get(payment.getRecordedBy()),
                payment.getComment(),
                payment.getExternalReference(),
                payment.getCancelReason(),
                payment.getCancelComment(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
