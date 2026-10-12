package kz.edu.soccerhub.common.port;
import java.util.*;
import java.math.BigDecimal;
public interface CompanyContractBalancePort { BigDecimal outstandingContract(UUID contractId); Map<UUID,BigDecimal> debtContracts(Set<UUID> branchIds); }
