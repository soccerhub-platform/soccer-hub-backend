package kz.edu.soccerhub.dispatcher.api;
import kz.edu.soccerhub.dispatcher.application.service.DispatcherOperationsService;
import kz.edu.soccerhub.common.dto.company.*;
import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;
import jakarta.validation.Valid;
import kz.edu.soccerhub.dispatcher.application.dto.operations.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.*;
import java.time.LocalDate;
@RestController @RequestMapping("/dispatcher/operations") @PreAuthorize("hasAuthority('DISPATCHER')") @RequiredArgsConstructor
public class DispatcherOperationsController {
 private final DispatcherOperationsService service;
 @GetMapping("/assignees") public Collection<AdminDto> assignees(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId){return service.assignees(UUID.fromString(jwt.getSubject()),branchId);}
 @GetMapping("/contracts/{id}") public CompanyEntry contract(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){return service.contractRecord(UUID.fromString(jwt.getSubject()),id);}
 @GetMapping("/contracts/{id}/follow-up") public ContractFollowUp followUp(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){return service.followUp(UUID.fromString(jwt.getSubject()),id);}
 @PostMapping("/contracts/{id}/follow-up") public void followUp(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody DispatcherFollowUpInput c){service.updateFollowUp(UUID.fromString(jwt.getSubject()),id,c.toCommand());}
 @GetMapping("/tasks") public OperationsPage tasks(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) UUID branchId,@RequestParam(defaultValue="LEAD") String kind,@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size){return service.tasks(UUID.fromString(jwt.getSubject()),branchId,kind,status,search,page,size);}
 @GetMapping("/capacity") public OperationsPage capacity(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) UUID branchId,@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size,@RequestParam(defaultValue="") String sort){return service.capacity(UUID.fromString(jwt.getSubject()),branchId,status,search,page,size,sort);}
 @GetMapping("/trials") public TrialCompanyPage trials(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) UUID branchId,@RequestParam(defaultValue="") String status,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate dateFrom,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate dateTo,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size){return service.trials(UUID.fromString(jwt.getSubject()),branchId,status,dateFrom,dateTo,page,size);}
 @GetMapping("/trials/{id}") public TrialWorkspace trial(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){return service.trial(UUID.fromString(jwt.getSubject()),id);}
 @PostMapping("/trials/{id}") public void trial(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody DispatcherTrialMutationInput c){service.updateTrial(UUID.fromString(jwt.getSubject()),id,c.toCommand());}
 @GetMapping("/reports") public OperationsReport report(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) UUID branchId,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate dateFrom,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate dateTo){return service.report(UUID.fromString(jwt.getSubject()),branchId,dateFrom,dateTo);}
 @GetMapping("/students/{id}") public StudentHistory student(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){return service.student(UUID.fromString(jwt.getSubject()),id);}
}
