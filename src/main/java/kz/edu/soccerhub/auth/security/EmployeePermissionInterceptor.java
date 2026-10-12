package kz.edu.soccerhub.auth.security;
import jakarta.servlet.http.*;
import kz.edu.soccerhub.common.domain.enums.*;
import kz.edu.soccerhub.common.exception.*;
import kz.edu.soccerhub.common.port.EmployeeAccessPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.*;
@Component
@RequiredArgsConstructor
public class EmployeePermissionInterceptor implements HandlerInterceptor {
 private final EmployeeAccessPort accessPort;
 public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler) {
  var auth=SecurityContextHolder.getContext().getAuthentication();
  if(!(auth instanceof JwtAuthenticationToken jwt) || !(handler instanceof HandlerMethod method)) return true;
  var access=accessPort.getAccess(UUID.fromString(jwt.getName()));
  if(!access.enabled()) throw new UnauthorizedException("Доступ сотрудника отключён");
  var tokenRoles=Optional.ofNullable(jwt.getToken().getClaimAsStringList("roles")).orElse(List.of());
  if(tokenRoles.stream().anyMatch(role->!access.roles().contains(Role.valueOf(role)))) throw new UnauthorizedException("Роль учётной записи изменена. Войдите снова");
  if(tokenRoles.contains("DISPATCHER") || tokenRoles.contains("SUPER_ADMIN")) return true;
  String controller=method.getBeanType().getSimpleName();
  if(controller.equals("AuthController") || controller.endsWith("ProfileController") || controller.equals("AdminBranchController")) return true;
  if(controller.equals("AdminOperationsController")) {
   String action=method.getMethod().getName();
   Set<EmployeePermission> requiredOps=switch(action) {
    case "followUp","assignees" -> Set.of(EmployeePermission.CONTRACTS_READ);
    case "update" -> Set.of(EmployeePermission.CONTRACTS_WRITE);
    case "capacity","trials" -> Set.of(EmployeePermission.SCHEDULE_READ);
    case "debts" -> Set.of(EmployeePermission.CONTRACTS_READ,EmployeePermission.PAYMENTS_READ);
    case "student" -> Set.of(EmployeePermission.CLIENTS_READ,EmployeePermission.CRM_READ,EmployeePermission.CONTRACTS_READ,EmployeePermission.PAYMENTS_READ,EmployeePermission.SCHEDULE_READ);
    case "reports" -> Set.of(EmployeePermission.ANALYTICS_READ,EmployeePermission.CRM_READ,EmployeePermission.CONTRACTS_READ,EmployeePermission.PAYMENTS_READ,EmployeePermission.SCHEDULE_READ);
    default -> Set.of(EmployeePermission.CRM_READ,EmployeePermission.CONTRACTS_READ,EmployeePermission.SCHEDULE_READ);
   };
   if(!access.permissions().containsAll(requiredOps))throw new org.springframework.security.access.AccessDeniedException("Нет прав для общей очереди или отчёта");
   return true;
  }
  String resource=null;
  if(controller.contains("Analytics") || controller.equals("AdminDashboardController")) resource="ANALYTICS";
  else if(controller.contains("Payment") || (controller.contains("Contract") && request.getServletPath().contains("/payments"))) resource="PAYMENTS";
  else if(controller.contains("Contract")) resource="CONTRACTS";
  else if(controller.contains("Lead")) resource="CRM";
  else if(controller.contains("Student") || controller.equals("ClientController") || controller.equals("AdminClientController")) resource="CLIENTS";
  else if(controller.startsWith("AdminCoach") || controller.equals("AdminTrainerController") || controller.equals("CoachController")) resource="TEAM";
  else if(controller.contains("Group") || controller.contains("Session") || controller.contains("Trial")) resource="SCHEDULE";
  if(tokenRoles.contains("COACH") && !tokenRoles.contains("ADMIN") && resource!=null && !controller.equals("CoachSessionController"))
   throw new org.springframework.security.access.AccessDeniedException("Раздел недоступен для роли тренера");
  boolean write=!Set.of("GET","HEAD","OPTIONS").contains(request.getMethod());
  EmployeePermission required=null;
  if(tokenRoles.contains("COACH") && controller.equals("CoachSessionController")) {
   required=write ? EmployeePermission.COACH_SESSIONS_WRITE : EmployeePermission.COACH_SESSIONS_READ;
  } else if(tokenRoles.contains("ADMIN") && resource!=null) {
   required=EmployeePermission.valueOf(resource+((write && !resource.equals("ANALYTICS"))?"_WRITE":"_READ"));
  }
  if(tokenRoles.contains("ADMIN") && controller.equals("AdminLeadController") && method.getMethod().getName().equals("convertLead") &&
      (!access.permissions().contains(EmployeePermission.CLIENTS_WRITE) || !access.permissions().contains(EmployeePermission.CONTRACTS_WRITE)))
   throw new org.springframework.security.access.AccessDeniedException("Для конверсии нужны права изменения учеников и договоров");
  if(tokenRoles.contains("ADMIN") && controller.equals("AdminStudentController") && method.getMethod().getName().equals("getContracts") && !access.permissions().contains(EmployeePermission.CONTRACTS_READ))
   throw new org.springframework.security.access.AccessDeniedException("Нет права просмотра договоров");
  if(required!=null && !access.permissions().contains(required)) throw new org.springframework.security.access.AccessDeniedException("Нет права на этот раздел или операцию");
  return true;
 }
}
