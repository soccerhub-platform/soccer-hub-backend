package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.payment.ContractPaymentSummaryOutput;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentSummaryQueryInput;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface ContractPaymentSummaryPort {

    ContractPaymentSummaryOutput getContractPaymentSummary(ContractPaymentSummaryQueryInput contract);

    Map<UUID, ContractPaymentSummaryOutput> getContractPaymentSummaries(Collection<ContractPaymentSummaryQueryInput> contracts);
}
