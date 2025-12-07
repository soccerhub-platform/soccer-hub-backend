package kz.edu.soccerhub.dispatcher.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kz.edu.soccerhub.common.domain.model.AbstractAuditableEntity;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "dispatcher_profiles")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DispatcherProfile extends AbstractAuditableEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id; // = app_user.id

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "phone", unique = true)
    private String phone;

}