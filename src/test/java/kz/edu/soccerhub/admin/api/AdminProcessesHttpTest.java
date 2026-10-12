package kz.edu.soccerhub.admin.api;
import kz.edu.soccerhub.common.domain.enums.*;
import kz.edu.soccerhub.common.dto.auth.EmployeeAccessDto;
import kz.edu.soccerhub.common.port.EmployeeAccessPort;
import kz.edu.soccerhub.admin.application.service.*;
import kz.edu.soccerhub.coach.application.service.CoachSessionService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.http.MediaType;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc
class AdminProcessesHttpTest {
 @Autowired MockMvc mvc;
 @MockitoBean EmployeeAccessPort access;
 @MockitoBean AdminOperationsService operations;
 @MockitoBean AdminTrialService trials;
 @MockitoBean CoachSessionService coach;
 UUID actor=UUID.randomUUID(),branch=UUID.randomUUID(),id=UUID.randomUUID();
 void allow(String role,Set<EmployeePermission> permissions) {when(access.getAccess(actor)).thenReturn(new EmployeeAccessDto(true,Set.of(Role.valueOf(role)),permissions));}
 RequestPostProcessor token(String role) {return jwt().jwt(j->j.subject(actor.toString()).claim("roles",List.of(role))).authorities(new SimpleGrantedAuthority(role));}
 @Test void adminReportAllowsAllTimeAndPassesBranch() throws Exception {
  allow("ADMIN",EnumSet.allOf(EmployeePermission.class));
  mvc.perform(get("/admin/operations/reports").param("branchId",branch.toString()).with(token("ADMIN"))).andExpect(status().isOk());
  verify(operations).report(actor,branch,null,null);
 }
 @Test void coachHistoryAllowsAllTimeWithServerFilters() throws Exception {
  allow("COACH",Set.of(EmployeePermission.COACH_SESSIONS_READ));
  mvc.perform(get("/coach/history").param("page","2").param("status","COMPLETED").param("sort","OLDEST").with(token("COACH"))).andExpect(status().isOk());
  verify(coach).getHistory(actor,null,null,2,20,null,"COMPLETED","","OLDEST");
 }
 @Test void adminTrialMutationsRequireVersion() throws Exception {
  allow("ADMIN",EnumSet.allOf(EmployeePermission.class));
  mvc.perform(post("/admin/trials/{id}/attendance",id).with(token("ADMIN")).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ATTENDED\"}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.version").exists());
  mvc.perform(post("/admin/trials/{id}/cancel",id).with(token("ADMIN")).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Не сможет прийти\"}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.version").exists());
  verifyNoInteractions(trials);
 }
 @Test void coachTrialMutationsRequireVersion() throws Exception {
  allow("COACH",EnumSet.allOf(EmployeePermission.class));
  mvc.perform(patch("/coach/sessions/{session}/trials/{id}/attendance",branch,id).with(token("COACH")).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ATTENDED\"}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.version").exists());
  mvc.perform(put("/coach/sessions/{session}/trials/{id}/recommendation",branch,id).with(token("COACH")).contentType(MediaType.APPLICATION_JSON).content("{\"recommendation\":\"RECOMMEND_ENROLLMENT\"}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.version").exists());verifyNoInteractions(coach);
 }
 @Test void aggregateQueueDoesNotBypassResourcePermissions() throws Exception {
  allow("ADMIN",Set.of(EmployeePermission.CRM_READ));
  mvc.perform(get("/admin/operations/tasks").param("branchId",branch.toString()).with(token("ADMIN"))).andExpect(status().isForbidden());verifyNoInteractions(operations);
 }
 @Test void readOnlyAdminCannotWriteFollowUp() throws Exception {
  allow("ADMIN",Set.of(EmployeePermission.CONTRACTS_READ));
  mvc.perform(post("/admin/operations/contracts/{id}/follow-up",id).with(token("ADMIN")).contentType(MediaType.APPLICATION_JSON).content("{\"operation\":\"COMPLETE\",\"version\":1}")).andExpect(status().isForbidden());verifyNoInteractions(operations);
 }
 @Test void coachCannotReadAdminFinance() throws Exception {
  allow("COACH",EnumSet.allOf(EmployeePermission.class));
  mvc.perform(get("/admin/operations/reports").param("branchId",branch.toString()).with(token("COACH"))).andExpect(status().isForbidden());verifyNoInteractions(operations);
 }
 @Test void disabledAdminCannotUseNewQueues() throws Exception {
  when(access.getAccess(actor)).thenReturn(new EmployeeAccessDto(false,Set.of(Role.ADMIN),EnumSet.allOf(EmployeePermission.class)));
  mvc.perform(get("/admin/operations/tasks").param("branchId",branch.toString()).with(token("ADMIN"))).andExpect(status().isUnauthorized());verifyNoInteractions(operations);
 }
}
