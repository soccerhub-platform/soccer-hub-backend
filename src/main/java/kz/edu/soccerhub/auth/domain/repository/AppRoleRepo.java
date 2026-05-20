package kz.edu.soccerhub.auth.domain.repository;

import kz.edu.soccerhub.auth.domain.model.AppRoleEntity;
import kz.edu.soccerhub.common.domain.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppRoleRepo extends JpaRepository<AppRoleEntity, Role> {
}
