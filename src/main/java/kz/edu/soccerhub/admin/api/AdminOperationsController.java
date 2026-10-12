package kz.edu.soccerhub.admin.api;
import kz.edu.soccerhub.admin.application.service.AdminOperationsService;
import kz.edu.soccerhub.common.dto.company.*;
import kz.edu.soccerhub.admin.application.dto.operations.AdminFollowUpInput;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.time.LocalDate;
import java.util.*;
@RestController @RequestMapping("/admin/operations") @PreAuthorize("hasAuthority('ADMIN')") @RequiredArgsConstructor
public class AdminOperationsController {
 private final AdminOperationsService service;
 private UUID actor(Jwt jwt) {return UUID.fromString(jwt.getSubject());}
 @GetMapping("/tasks") public OperationsPage tasks(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@RequestParam(defaultValue="ALL") String kind,@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="false") boolean mine,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size) {return service.tasks(actor(jwt),branchId,kind,status,search,mine,page,size);}
 @GetMapping("/assignees") public Collection<kz.edu.soccerhub.common.dto.admin.AdminDto> assignees(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId) {return service.assignees(actor(jwt),branchId);}
 @GetMapping("/contracts/{id}/follow-up") public ContractFollowUp followUp(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) {return service.followUp(actor(jwt),id);}
 @PostMapping("/contracts/{id}/follow-up") public void update(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@jakarta.validation.Valid @RequestBody AdminFollowUpInput input) {service.updateFollowUp(actor(jwt),id,input);}
 @GetMapping("/trials") public TrialCompanyPage trials(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@RequestParam(defaultValue="") String status,@RequestParam(required=false) LocalDate dateFrom,@RequestParam(required=false) LocalDate dateTo,@RequestParam(defaultValue="NEWEST") String sort,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {return service.trials(actor(jwt),branchId,status,dateFrom,dateTo,sort,page,size);}
 @GetMapping("/students/{id}") public StudentHistory student(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestParam UUID branchId) {return service.student(actor(jwt),branchId,id);}
 @GetMapping("/capacity") public OperationsPage capacity(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="") String sort,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size) {return service.capacity(actor(jwt),branchId,status,search,sort,page,size);}
 @GetMapping("/debts") public kz.edu.soccerhub.admin.application.dto.operations.AdminDebtPage debts(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="") String sort,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size) {return service.debts(actor(jwt),branchId,search,status,sort,page,size);}
 @GetMapping("/reports") public OperationsReport reports(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@RequestParam(required=false) LocalDate dateFrom,@RequestParam(required=false) LocalDate dateTo) {return service.report(actor(jwt),branchId,dateFrom,dateTo);}
}
