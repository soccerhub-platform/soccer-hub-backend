package kz.edu.soccerhub.common.port;
import kz.edu.soccerhub.common.dto.company.*;
import java.util.*;
public interface CompanyPaymentHistoryPort { List<CompanyEntry> payments(Set<UUID> contractIds);List<DebtSnapshot> debtTrend(CompanyQuery query); }
