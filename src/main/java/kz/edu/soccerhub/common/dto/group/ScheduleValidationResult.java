package kz.edu.soccerhub.common.dto.group;

import java.util.List;

public record ScheduleValidationResult(
        boolean valid,
        List<ScheduleValidationConflict> conflicts
) {
    public static ScheduleValidationResult of(List<ScheduleValidationConflict> conflicts) {
        return new ScheduleValidationResult(conflicts == null || conflicts.isEmpty(), conflicts == null ? List.of() : conflicts);
    }
}
