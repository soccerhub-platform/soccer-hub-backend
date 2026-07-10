package kz.edu.soccerhub.admin.application.dto.coach;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminTrainerListOutput(
        List<Item> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public record Item(
            UUID id,
            String firstName,
            String lastName,
            String email,
            String phone,
            String specialization,
            String accountStatus,
            String workStatus,
            int groupCount,
            int todaySessionsCount,
            Load load,
            Reports reports
    ) {}

    public record Load(
            int completed,
            int planned,
            int used,
            int limit,
            int percentage,
            String status
    ) {}

    public record Reports(
            int overdueCount,
            int pendingCount,
            LocalDateTime lastReportAt
    ) {}
}
