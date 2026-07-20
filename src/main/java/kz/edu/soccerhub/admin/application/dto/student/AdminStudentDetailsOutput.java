package kz.edu.soccerhub.admin.application.dto.student;

import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionAttendanceStatus;
import kz.edu.soccerhub.common.dto.media.MediaAssetResponse;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentStatus;
import kz.edu.soccerhub.payments.domain.enums.PaymentMethod;
import kz.edu.soccerhub.payments.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AdminStudentDetailsOutput(
        PlayerBlock player,
        ClientBlock client,
        CurrentGroupBlock currentGroup,
        CurrentContractBlock currentContract,
        AttendanceSummaryBlock attendanceSummary,
        List<RecentPaymentBlock> recentPayments,
        List<RecentAttendanceBlock> recentAttendance,
        List<AdminStudentRiskOutput> risks,
        CapabilitiesBlock capabilities
) {
    public record PlayerBlock(
            UUID id,
            String fullName,
            String firstName,
            String lastName,
            LocalDate birthDate,
            Integer age,
            String position,
            MediaAssetResponse avatar
    ) {
    }

    public record ClientBlock(
            UUID id,
            String fullName,
            String phone,
            String email,
            String status
    ) {
    }

    public record CurrentGroupBlock(
            UUID id,
            String name,
            MediaAssetResponse avatar,
            String coachName,
            String scheduleLabel,
            UUID nextSessionId,
            LocalDateTime nextSessionAt
    ) {
    }

    public record CurrentContractBlock(
            UUID id,
            String contractNumber,
            ContractStatus status,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal amount,
            String currency,
            ContractPaymentStatus paymentStatus,
            BigDecimal paidAmount,
            BigDecimal outstandingAmount,
            BigDecimal overpaidAmount,
            LocalDateTime lastPaidAt
    ) {
    }

    public record AttendanceSummaryBlock(
            Integer attendanceRate,
            Integer presentCount,
            Integer absentCount,
            Integer lateCount,
            Integer missedLast30Days
    ) {
    }

    public record RecentPaymentBlock(
            UUID id,
            BigDecimal amount,
            String currency,
            PaymentMethod method,
            PaymentStatus status,
            LocalDateTime paidAt,
            String comment
    ) {
    }

    public record RecentAttendanceBlock(
            UUID sessionId,
            LocalDate date,
            String groupName,
            TrainingSessionAttendanceStatus status
    ) {
    }

    public record CapabilitiesBlock(
            boolean canEdit,
            boolean canManageAvatar
    ) {
    }
}
