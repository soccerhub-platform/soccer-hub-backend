package kz.edu.soccerhub.admin.application.dto.student;

public record AdminStudentRiskOutput(
        AdminStudentRiskCode code,
        String label,
        AdminStudentRiskSeverity severity
) {
}
