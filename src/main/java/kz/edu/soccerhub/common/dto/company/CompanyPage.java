package kz.edu.soccerhub.common.dto.company;
import lombok.Builder;
import java.util.List;
@Builder
public record CompanyPage(List<CompanyEntry> content, long totalElements, int totalPages, int number, int size) {}
