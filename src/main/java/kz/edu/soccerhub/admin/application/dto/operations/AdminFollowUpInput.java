package kz.edu.soccerhub.admin.application.dto.operations;
import jakarta.validation.constraints.*;
import kz.edu.soccerhub.common.dto.company.FollowUpCommand;
import lombok.Builder;
import java.time.*;
import java.util.UUID;
@Builder
public record AdminFollowUpInput(@NotNull FollowUpCommand.Operation operation,@NotNull @Min(0) Long version,UUID ownerId,@Size(max=240) String nextAction,Instant dueAt,@Size(max=40) String outcome,LocalDate promisedPaymentDate,@Size(max=2000) String comment) {
 public FollowUpCommand toCommand() {return FollowUpCommand.builder().operation(operation).version(version).ownerId(ownerId).nextAction(nextAction).dueAt(dueAt).outcome(outcome).promisedPaymentDate(promisedPaymentDate).comment(comment).build();}
}
