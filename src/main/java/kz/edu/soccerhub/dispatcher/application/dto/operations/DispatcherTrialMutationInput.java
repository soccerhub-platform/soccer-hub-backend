package kz.edu.soccerhub.dispatcher.application.dto.operations;
import jakarta.validation.constraints.*;
import kz.edu.soccerhub.common.dto.company.TrialMutationCommand;
import kz.edu.soccerhub.trial.domain.enums.*;
import lombok.Builder;
import java.time.LocalDateTime;
@Builder
public record DispatcherTrialMutationInput(@NotNull TrialMutationCommand.Operation operation,@NotNull @Min(0) Long version,TrialAttendanceStatus attendance,TrialResult result,TrialNextActionType nextActionType,LocalDateTime nextActionAt,@Size(max=2000) String comment) {
 public TrialMutationCommand toCommand() {return TrialMutationCommand.builder().operation(operation).version(version).attendance(attendance).result(result).nextActionType(nextActionType).nextActionAt(nextActionAt).comment(comment).build();}
}
