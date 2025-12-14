package kz.edu.soccerhub.admin.domain.model;

import jakarta.persistence.*;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "admin_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
public class AdminProfile extends AbstractAuditableEntity {

    @Id
    @Column(name = "id")
    private UUID id;

    private String firstName;

    private String lastName;

    private String phone;

    private String email;

    private Boolean active;

    @OneToMany(mappedBy = "admin",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY,
            orphanRemoval = true)
    private List<AdminBranch> adminBranches;

}
