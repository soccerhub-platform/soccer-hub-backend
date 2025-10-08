package kz.edu.soccerhub.auth.domain.model;

import jakarta.persistence.*;
import kz.edu.soccerhub.common.domain.enums.Role;
import lombok.*;

// AppRole.java
@Entity
@Table(name="app_role")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AppRoleEntity {

  @Id
  @Column(length=50)
  @Enumerated(EnumType.STRING)
  private Role code;

  @Column(name = "description")
  private String description;

}