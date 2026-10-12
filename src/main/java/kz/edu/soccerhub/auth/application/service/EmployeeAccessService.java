package kz.edu.soccerhub.auth.application.service;
import kz.edu.soccerhub.auth.domain.repository.AppUserRepo;
import kz.edu.soccerhub.common.domain.enums.*;
import kz.edu.soccerhub.common.dto.auth.*;
import kz.edu.soccerhub.common.exception.*;
import kz.edu.soccerhub.common.port.EmployeeAccessPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class EmployeeAccessService implements EmployeeAccessPort {
 private final AppUserRepo users;
 private final NamedParameterJdbcTemplate jdbc;
 private Set<EmployeePermission> available(Set<Role> roles) {
  return Arrays.stream(EmployeePermission.values()).filter(p ->
   (roles.contains(Role.ADMIN) && !p.name().startsWith("COACH_")) || (roles.contains(Role.COACH) && p.name().startsWith("COACH_")))
   .collect(Collectors.toSet());
 }
 @Transactional(readOnly=true)
 public EmployeeAccessDto getAccess(UUID id) {
  var user=users.findById(id).orElseThrow(()->new UnauthorizedException("Учётная запись недоступна"));
  var roles=user.getRoles().stream().map(role->role.getCode()).collect(Collectors.toSet());
  var params=new MapSqlParameterSource("id",id);
  boolean configured=Boolean.TRUE.equals(jdbc.queryForObject("select exists(select 1 from employee_access_policy where user_id=:id)",params,Boolean.class));
  var permissions=configured ? jdbc.query("select permission from employee_permission where user_id=:id",params,(rs,n)->EmployeePermission.valueOf(rs.getString(1))).stream().collect(Collectors.toSet()) : available(roles);
  permissions.retainAll(available(roles));
  return new EmployeeAccessDto(user.isEnabled(),roles,permissions);
 }
 @Transactional
 public void changePermissions(ChangeEmployeePermissionsCommand command) {
  // Serialize concurrent permission edits for the same account.
  jdbc.queryForObject("select id from app_user where id=:id for update",new MapSqlParameterSource("id",command.userId()),UUID.class);
  var access=getAccess(command.userId());
  if(access.roles().contains(Role.DISPATCHER) || access.roles().contains(Role.SUPER_ADMIN)) throw new BadRequestException("Права руководителя не редактируются через карточку сотрудника");
  var grants=command.permissions();
  if(grants==null || !available(access.roles()).containsAll(grants)) throw new BadRequestException("Указаны права, недоступные для роли сотрудника");
  for(var p:grants) if(p.name().endsWith("_WRITE") && !grants.contains(EmployeePermission.valueOf(p.name().replace("_WRITE","_READ"))))
   throw new BadRequestException("Для изменения нужен доступ к просмотру раздела");
  var params=new MapSqlParameterSource("id",command.userId()).addValue("actor",command.changedBy());
  jdbc.update("insert into employee_access_policy(user_id,changed_by) values (:id,:actor) on conflict(user_id) do update set changed_by=:actor,updated_at=now()",params);
  jdbc.update("delete from employee_permission where user_id=:id",params);
  for(var grant:grants) jdbc.update("insert into employee_permission(user_id,permission) values (:id,:permission)",new MapSqlParameterSource("id",command.userId()).addValue("permission",grant.name()));
  jdbc.update("insert into employee_access_history(id,user_id,changed_by,permissions) values (:event,:id,:actor,:permissions)",params.addValue("event",UUID.randomUUID()).addValue("permissions",grants.stream().map(Enum::name).sorted().collect(Collectors.joining(","))));
 }
}
