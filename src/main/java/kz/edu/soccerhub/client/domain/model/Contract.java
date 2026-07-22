package kz.edu.soccerhub.client.domain.model;

import jakarta.persistence.*;
import kz.edu.soccerhub.client.domain.enums.ContractCancelReasonCode;
import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import kz.edu.soccerhub.crm.domain.model.enums.LeadType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "contracts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contract extends AbstractAuditableEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "client_id", nullable = false)
    private UUID clientId;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "contract_number", nullable = false)
    private String contractNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "lead_type", nullable = false)
    private LeadType leadType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ContractStatus status;

    @Column(name = "coach_id")
    private UUID coachId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "notes")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "cancel_reason_code")
    private ContractCancelReasonCode cancelReasonCode;

    @Column(name = "cancel_comment")
    private String cancelComment;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
