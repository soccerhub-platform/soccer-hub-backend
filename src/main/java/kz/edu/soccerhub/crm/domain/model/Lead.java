package kz.edu.soccerhub.crm.domain.model;

import jakarta.persistence.*;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import kz.edu.soccerhub.crm.domain.model.enums.Gender;
import kz.edu.soccerhub.crm.domain.model.enums.LeadSource;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    @OneToOne(mappedBy = "lead", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private LeadTrial trial;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "player_id")
    private UUID playerId;

    @Column(name = "contract_id")
    private UUID contractId;

    @Column(name = "lost_reason_code")
    private String lostReasonCode;

    @Column(name = "lost_comment", columnDefinition = "TEXT")
    private String lostComment;

    @Column(name = "lost_at")
    private LocalDateTime lostAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lost_reason_code", referencedColumnName = "code", insertable = false, updatable = false)
    private LeadLossReasonEntity lostReason;

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
            LocalDate trialDate,
            LocalTime startTime,
            LocalTime endTime,
            String comment
    ) {
        if (this.trial == null) {
            this.trial = LeadTrial.builder()
                    .id(UUID.randomUUID())
                    .build();
            this.trial.attachToLead(this);
        }

        this.trial.schedule(
                childId,
                groupId,
                coachId,
                trialDate,
                startTime,
                endTime,
                comment
        );
    }

    public void markConverted(UUID clientId, UUID playerId, UUID contractId) {
        this.clientId = clientId;
        this.playerId = playerId;
        this.contractId = contractId;
    }

    public void markLost(String reasonCode, String comment, LocalDateTime at) {
        this.lostReasonCode = reasonCode;
        this.lostComment = comment;
        this.lostAt = at;
    }

    public void clearLostSnapshot() {
        this.lostReasonCode = null;
        this.lostComment = null;
        this.lostAt = null;
    }

    public void addChild(String childName, Integer childAge, Gender gender, String experience) {
        LeadChild leadChild = LeadChild.builder()
                .id(UUID.randomUUID())
                .lead(this)
                .childName(childName)
                .childAge(childAge)
                .gender(gender)
                .experience(experience)
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
