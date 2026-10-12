package kz.edu.soccerhub.common.dto.company;
import jakarta.validation.constraints.*;
import kz.edu.soccerhub.trial.domain.enums.*;
import lombok.Builder;
import java.time.LocalDateTime;
@Builder
public record TrialMutationCommand(@NotNull Operation operation,@NotNull @Min(0) Long version,TrialAttendanceStatus attendance,TrialResult result,TrialNextActionType nextActionType,LocalDateTime nextActionAt,@Size(max=2000) String comment) {public enum Operation {ATTENDANCE,RESULT}}
