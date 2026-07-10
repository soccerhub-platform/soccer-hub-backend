package kz.edu.soccerhub.coach.domain.model;

import jakarta.persistence.*;
import kz.edu.soccerhub.coach.domain.model.enums.AccountStatus;
import kz.edu.soccerhub.coach.domain.model.enums.WorkStatus;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "coach_profiles")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CoachProfile extends AbstractAuditableEntity {

    @Id
    private UUID id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "specialization")
    private String specialization;

    @Column(name = "bio")
    private String bio;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status")
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "work_status")
    private WorkStatus workStatus = WorkStatus.AVAILABLE;

    @Column(name = "vacation_from")
    private LocalDate vacationFrom;

    @Column(name = "vacation_to")
    private LocalDate vacationTo;

    @Column(name = "work_status_reason")
    private String workStatusReason;

    @OneToMany(mappedBy = "coachProfile", cascade =  CascadeType.ALL, orphanRemoval = true)
    private Set<CoachBranch> coachBranches = new HashSet<>();

}
