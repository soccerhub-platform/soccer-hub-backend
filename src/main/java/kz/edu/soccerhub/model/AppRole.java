package kz.edu.soccerhub.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

// AppRole.java
@Entity
@Table(name="app_role")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AppRole {

  @Id
  @Column(length=50)
  private String code;

  @Column(name = "description")
  private String description;

}