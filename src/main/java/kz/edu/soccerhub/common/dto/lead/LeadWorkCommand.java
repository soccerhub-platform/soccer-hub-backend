package kz.edu.soccerhub.common.dto.lead;
import jakarta.validation.constraints.*;
import lombok.Builder;
import java.time.Instant;
@Builder
public record LeadWorkCommand(
        @NotNull Operation operation,
        @NotNull @Min(0) Long version,
        @NotNull Priority priority,
        @Size(max = 240) String nextAction,
        Instant nextActionAt,
        Channel channel,
        Outcome outcome,
        @Size(max = 2000) String comment) {
    public enum Operation { PLAN, CONTACT, COMPLETE }
    public enum Priority { NORMAL, HIGH, URGENT }
    public enum Channel { PHONE, WHATSAPP, EMAIL, IN_PERSON }
    public enum Outcome { REACHED, NO_ANSWER, FOLLOW_UP, WRONG_NUMBER }
}
