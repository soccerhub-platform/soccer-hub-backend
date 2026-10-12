package kz.edu.soccerhub.common.dto.company;
import kz.edu.soccerhub.common.dto.branch.BranchDto;
import lombok.Builder;
import java.time.LocalDate;
import java.util.List;
@Builder
public record CompanySummary(List<BranchDto> branches,List<CompanyMetric> metrics,LocalDate dateFrom,LocalDate dateTo) {}
