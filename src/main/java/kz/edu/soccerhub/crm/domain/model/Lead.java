package kz.edu.soccerhub.crm.domain.model;

import jakarta.persistence.*;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import kz.edu.soccerhub.crm.domain.model.enums.Gender;
import kz.edu.soccerhub.crm.domain.model.enums.LeadSource;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.crm.domain.model.enums.LeadType;
import kz.edu.soccerhub.crm.domain.model.enums.TimePreference;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "lead_type", nullable = false)
    private LeadType leadType;

    @Column(name = "primary_contact_name", nullable = false)
    private String primaryContactName;

    @Column(name = "primary_contact_phone", nullable = false)
    private String primaryContactPhone;

    @Column(name = "primary_contact_email")
    private String primaryContactEmail;


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

    @Enumerated(EnumType.STRING)
    @Column(name = "time_preference")
    private TimePreference timePreference;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToOne(mappedBy = "lead", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private LeadTrial trial;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(name = "participant_id")
    private UUID participantId;

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
    private List<LeadParticipant> participants = new ArrayList<>();

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

    public void updateQualificationFields(
            String preferredDays,
            String experience,
            TimePreference timePreference,
            String notes
    ) {
        this.preferredDays = preferredDays;
        this.experience = experience;
        this.timePreference = timePreference;
        this.notes = notes;
    }

    public void scheduleTrial(
            UUID participantId,
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
                participantId,
                groupId,
                coachId,
                trialDate,
                startTime,
                endTime,
                comment
        );
    }

    public void markConverted(UUID clientId, UUID participantId, UUID contractId) {
        this.clientId = clientId;
        this.participantId = participantId;
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

    public void addParticipant(String fullName, java.time.LocalDate birthDate, Gender gender, String experience) {
        LeadParticipant leadParticipant = LeadParticipant.builder()
                .id(UUID.randomUUID())
                .lead(this)
                .fullName(fullName)
                .birthDate(birthDate)
                .gender(gender)
                .experience(experience)
                .build();
        this.participants.add(leadParticipant);
    }

    public void clearParticipants() {
        this.participants.clear();
    }

    public boolean isReadyForConversion() {
        if (primaryContactName == null || primaryContactName.isBlank()) {
            return false;
        }
        if (primaryContactPhone == null || primaryContactPhone.isBlank()) {
            return false;
        }
        if (participants == null || participants.isEmpty()) {
            return false;
        }

        for (LeadParticipant participant : participants) {
            if (participant.getFullName() == null || participant.getFullName().isBlank()) {
                return false;
            }
        }

        return true;
    }

}
