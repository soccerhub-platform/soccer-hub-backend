package kz.edu.soccerhub.common.dto.company;
import lombok.Builder;
import java.math.BigDecimal;
import java.util.UUID;
@Builder
public record CompanyMetric(UUID branchId, String key, long count, String currency, BigDecimal amount) {}
