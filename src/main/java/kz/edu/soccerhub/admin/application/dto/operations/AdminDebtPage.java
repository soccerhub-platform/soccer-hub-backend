package kz.edu.soccerhub.admin.application.dto.operations;
import kz.edu.soccerhub.common.dto.company.*;
import java.util.*;
@lombok.Builder
public record AdminDebtPage(List<CompanyEntry> content,long totalElements,int totalPages,int number,int size,List<CompanyMetric> totals,Map<String,Long> summary) {}
