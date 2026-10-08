package kz.edu.soccerhub.admin.api.dto;

import jakarta.validation.constraints.*;
import kz.edu.soccerhub.common.dto.lead.LeadWorkCommand;
import lombok.Builder;
import java.time.Instant;

@Builder
public record AdminLeadWorkInput(
        @NotNull LeadWorkCommand.Operation operation,
        @NotNull @Min(0) Long version,
        @NotNull LeadWorkCommand.Priority priority,
        @Size(max = 240) String nextAction,
        Instant nextActionAt,
        LeadWorkCommand.Channel channel,
        LeadWorkCommand.Outcome outcome,
        @Size(max = 2000) String comment) {
    public LeadWorkCommand toCommand() {
        return LeadWorkCommand.builder().operation(operation).version(version).priority(priority)
                .nextAction(nextAction).nextActionAt(nextActionAt).channel(channel).outcome(outcome)
                .comment(comment).build();
    }
}
