package kz.edu.soccerhub.common.dto.lead;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public record LeadQualificationInput(
        @Size(max = 255, message = "Child name is too long")
        String childName,

        @Min(value = 3, message = "Child age must be at least 3")
        @Max(value = 18, message = "Child age must be at most 18")
        Integer childAge,

        @Size(max = 100, message = "Experience value is too long")
        String experience,

        List<String> preferredDays,

        @Size(max = 100, message = "Preferred time value is too long")
        String preferredTime,

        @Size(max = 1000, message = "Comment is too long")
        String comment
) {
}

