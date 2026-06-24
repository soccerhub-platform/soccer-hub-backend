package kz.edu.soccerhub.payments.application;

import kz.edu.soccerhub.common.dto.payment.ContractPaymentSummaryOutput;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentSummaryQueryInput;
import kz.edu.soccerhub.common.port.ContractPaymentSummaryPort;
import kz.edu.soccerhub.payments.domain.model.Payment;
import kz.edu.soccerhub.payments.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractPaymentSummaryService implements ContractPaymentSummaryPort {

    private final PaymentRepository paymentRepository;
    private final ContractPaymentCalculator contractPaymentCalculator;

    @Override
    @Transactional(readOnly = true)
    public ContractPaymentSummaryOutput getContractPaymentSummary(ContractPaymentSummaryQueryInput contract) {
        List<Payment> payments = paymentRepository.findByContractIdOrderByPaidAtDescCreatedAtDesc(contract.contractId());
        return contractPaymentCalculator.summarize(contract.contractId(), contract.contractAmount(), payments);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, ContractPaymentSummaryOutput> getContractPaymentSummaries(Collection<ContractPaymentSummaryQueryInput> contracts) {
        if (contracts == null || contracts.isEmpty()) {
            return Map.of();
        }

        Map<UUID, ContractPaymentSummaryQueryInput> queryByContractId = contracts.stream()
                .collect(Collectors.toMap(
                        ContractPaymentSummaryQueryInput::contractId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        Map<UUID, List<Payment>> paymentsByContractId = paymentRepository.findByContractIdInOrderByPaidAtDescCreatedAtDesc(queryByContractId.keySet())
                .stream()
                .collect(Collectors.groupingBy(Payment::getContractId, LinkedHashMap::new, Collectors.toList()));

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
}
