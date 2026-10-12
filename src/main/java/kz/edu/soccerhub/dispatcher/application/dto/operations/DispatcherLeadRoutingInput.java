package kz.edu.soccerhub.dispatcher.application.dto.operations;
import jakarta.validation.constraints.*;
import kz.edu.soccerhub.common.dto.lead.LeadRoutingCommand;
import lombok.Builder;
import java.util.UUID;
@Builder
public record DispatcherLeadRoutingInput(@NotNull UUID branchId,UUID adminId,@NotNull @Min(0) Long version,@NotBlank @Size(max=1000) String reason) {
 public LeadRoutingCommand toCommand() {return LeadRoutingCommand.builder().branchId(branchId).adminId(adminId).version(version).reason(reason).build();}
}
