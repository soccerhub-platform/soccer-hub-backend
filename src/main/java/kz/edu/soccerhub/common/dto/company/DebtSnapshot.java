package kz.edu.soccerhub.common.dto.company;
import lombok.Builder;
import java.util.UUID;
import java.time.LocalDate;
import java.math.BigDecimal;
@Builder
public record DebtSnapshot(UUID branchId,LocalDate date,String currency,long count,BigDecimal amount) {}
