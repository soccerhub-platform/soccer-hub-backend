package kz.edu.soccerhub.crm.application.service;
import kz.edu.soccerhub.common.port.AdminBranchAccessPort;
import kz.edu.soccerhub.common.port.LeadPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import java.util.UUID;
@Component("leadReadAccess")
@RequiredArgsConstructor
public class LeadReadAccess {
    private final LeadPort leads;
    private final AdminBranchAccessPort access;
    public boolean canRead(Jwt jwt, UUID leadId) {
        requireBranch(jwt, leads.getLeadBranchId(leadId));
        return true;
    }
    public void requireBranch(Jwt jwt, UUID branchId) {
        // Dispatcher / super-admin access is governed by their existing entry-point roles.
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean admin = authentication != null && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ADMIN"));
        if (admin && (branchId == null || !access.verifyAdminBelongsToBranch(UUID.fromString(jwt.getSubject()), branchId)))
            throw new AccessDeniedException("Выберите доступный филиал");
    }
}
