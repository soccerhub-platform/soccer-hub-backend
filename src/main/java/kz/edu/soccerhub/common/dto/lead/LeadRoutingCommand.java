package kz.edu.soccerhub.common.dto.lead;
import jakarta.validation.constraints.*;
import lombok.Builder;
import java.util.UUID;
@Builder
public record LeadRoutingCommand(@NotNull UUID branchId, UUID adminId, @NotNull @Min(0) Long version, @NotBlank @Size(max=1000) String reason) {}
