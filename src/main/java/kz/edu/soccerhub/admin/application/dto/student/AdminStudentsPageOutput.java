package kz.edu.soccerhub.admin.application.dto.student;

import java.util.List;

public record AdminStudentsPageOutput(
        Summary summary,
        List<AdminStudentListItemOutput> content,
        long totalElements,
        int totalPages,
        int number,
        int size
) {
    public record Summary(
            int total,
            int paid,
            int partiallyPaid,
            int unpaid,
            int withDebt,
            int withRisks,
            int withoutGroup,
            int lowAttendance,
            int expiredContracts,
            int endingSoon
    ) {
    }
}
