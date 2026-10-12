package kz.edu.soccerhub.common.dto.company;
import lombok.Builder;
import java.util.List;
@Builder
public record OperationsReport(CompanySummary current,CompanySummary previous,List<CompanyMetric> pipeline,List<DebtSnapshot> debtTrend,java.util.Map<String,String> lossReasonNames) {}
