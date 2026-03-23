package kz.edu.soccerhub.crm.domain.model;

import jakarta.persistence.*;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import kz.edu.soccerhub.crm.domain.model.enums.LeadSource;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "leads")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Lead extends AbstractAuditableEntity {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "parent_name", nullable = false)
    private String parentName;

    @Column(nullable = false)
    private String phone;

    private String email;

    @Column(name = "child_name")
    private String childName;

    @Column(name = "child_age")
    private Integer childAge;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadStatus status = LeadStatus.NEW;

    @Column(name = "assigned_admin_id")
    private UUID assignedAdminId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "qualification_data", columnDefinition = "TEXT")
    private String qualificationData;

    @Column(name = "trial_group_id")
    private UUID trialGroupId;

    @Column(name = "trial_coach_id")
    private UUID trialCoachId;

    @Column(name = "trial_date")
    private LocalDateTime trialDate;

    public void assignAdmin(UUID adminId) {
        this.assignedAdminId = adminId;
    }

    public void updateStatus(LeadStatus newStatus) {
        this.status = newStatus;
    }

    public void updateQualificationData(String qualificationData) {
        this.qualificationData = qualificationData;
    }

    public void scheduleTrial(UUID groupId, UUID coachId, LocalDateTime trialDate) {
        this.trialGroupId = groupId;
        this.trialCoachId = coachId;
        this.trialDate = trialDate;
    }

}