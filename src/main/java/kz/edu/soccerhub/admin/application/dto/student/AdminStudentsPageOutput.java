package kz.edu.soccerhub.admin.application.dto.student;

import java.util.List;

public record AdminStudentsPageOutput(
        List<AdminStudentListItemOutput> content,
        long totalElements,
        int totalPages,
        int number,
        int size
) {
}
