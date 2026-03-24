package kz.edu.soccerhub.crm.domain.model;

import jakarta.persistence.*;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import kz.edu.soccerhub.crm.domain.model.enums.LeadSource;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @Column(name = "preferred_days")
    private String preferredDays;

    @Column(name = "experience")
    private String experience;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "trial_group_id")
    private UUID trialGroupId;

    @Column(name = "trial_child_id")
    private UUID trialChildId;

    @Column(name = "trial_coach_id")
    private UUID trialCoachId;

    @Column(name = "trial_date")
    private LocalDateTime trialDate;

    @Column(name = "trial_duration_minutes")
    private Integer trialDurationMinutes;

    @Column(name = "trial_comment", columnDefinition = "TEXT")
    private String trialComment;

    @Column(name = "client_id")
    private UUID clientId;

    @Builder.Default
    @OneToMany(mappedBy = "lead", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LeadChild> children = new ArrayList<>();

    public void assignAdmin(UUID adminId) {
        this.assignedAdminId = adminId;
    }

    public void updateStatus(LeadStatus newStatus) {
        this.status = newStatus;
    }

    public void updateQualificationData(String qualificationData) {
        this.qualificationData = qualificationData;
    }

    public void updateQualificationFields(String preferredDays, String experience, String notes) {
        this.preferredDays = preferredDays;
        this.experience = experience;
        this.notes = notes;
    }

    public void scheduleTrial(
            UUID childId,
            UUID groupId,
            UUID coachId,
            LocalDateTime trialDate,
            Integer durationMinutes,
            String comment
    ) {
        this.trialChildId = childId;
        this.trialGroupId = groupId;
        this.trialCoachId = coachId;
        this.trialDate = trialDate;
        this.trialDurationMinutes = durationMinutes;
        this.trialComment = comment;
    }

    public void markConverted(UUID clientId) {
        this.clientId = clientId;
    }

    public void addChild(String childName, Integer childAge) {
        LeadChild leadChild = LeadChild.builder()
                .id(UUID.randomUUID())
                .lead(this)
                .childName(childName)
                .childAge(childAge)
                .build();
        this.children.add(leadChild);
    }

    public void clearChildren() {
        this.children.clear();
    }

    public boolean isReadyForConversion() {
        if (parentName == null || parentName.isBlank()) {
            return false;
        }
        if (phone == null || phone.isBlank()) {
            return false;
        }
        if (children == null || children.isEmpty()) {
            return false;
        }

        for (LeadChild child : children) {
            if (child.getChildName() == null || child.getChildName().isBlank()) {
                return false;
            }
            if (child.getChildAge() == null) {
                return false;
            }
        }

        return true;
    }

}