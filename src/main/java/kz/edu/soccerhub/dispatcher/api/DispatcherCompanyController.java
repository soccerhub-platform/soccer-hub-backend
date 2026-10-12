package kz.edu.soccerhub.dispatcher.api;
import kz.edu.soccerhub.dispatcher.application.service.DispatcherCompanyService;
import kz.edu.soccerhub.common.dto.company.CompanyPage;
import kz.edu.soccerhub.common.dto.company.CompanySummary;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;
@RestController
@RequestMapping("/dispatcher/company")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('DISPATCHER')")
public class DispatcherCompanyController {
 private final DispatcherCompanyService service;
 @GetMapping("/summary")
 public CompanySummary summary(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) UUID branchId,
  @RequestParam(required=false) LocalDate dateFrom,@RequestParam(required=false) LocalDate dateTo) {
  return service.summary(UUID.fromString(jwt.getSubject()),branchId,dateFrom,dateTo);
 }
 @GetMapping("/{kind:students|contracts|payments|leads|debts}")
 public CompanyPage page(@AuthenticationPrincipal Jwt jwt,@PathVariable String kind,
  @RequestParam(required=false) UUID branchId,@RequestParam(defaultValue="") String search,
  @RequestParam(defaultValue="") String status,@RequestParam(defaultValue="") String attention,@RequestParam(required=false) LocalDate dateFrom,
  @RequestParam(required=false) LocalDate dateTo,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size,@RequestParam(defaultValue="") String sort) {
  return service.page(UUID.fromString(jwt.getSubject()),kind,branchId,search,status,dateFrom,dateTo,page,size,attention,sort);
 }
}
