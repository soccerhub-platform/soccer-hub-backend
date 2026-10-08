package kz.edu.soccerhub.admin.application.service;
import kz.edu.soccerhub.common.dto.lead.*;
import kz.edu.soccerhub.admin.api.dto.AdminLeadWorkInput;
import kz.edu.soccerhub.common.dto.trial.TrialBookingListItemDto;
import kz.edu.soccerhub.common.port.*;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class AdminLeadWorkspaceService {
    private final LeadPort leads;
    private final LeadWorkPort work;
    private final TrialPort trials;
    private final AdminBranchAccessPort access;
    @Transactional
    public LeadOutput updatePreferences(UUID actor, UUID id,
            kz.edu.soccerhub.admin.api.dto.AdminLeadPreferencesInput input) {
        check(actor, leads.getLeadBranchId(id));
        work.updatePreferences(id, actor, LeadPreferencesCommand.builder()
                .version(input.version()).preferredDays(input.preferredDays())
                .timePreference(input.timePreference()).experience(input.experience()).notes(input.notes()).build());
        return detail(actor, id);
    }
    @Transactional(readOnly = true)
    public LeadKanbanOutput get(UUID actor, UUID branchId) {
        check(actor, branchId);
        Map<LeadStatus, List<LeadOutput>> columns = leads.getKanban(branchId, actor);
        List<UUID> ids = columns.values().stream().flatMap(Collection::stream).map(LeadOutput::id).toList();
        Map<UUID, List<TrialBookingListItemDto>> byLead = trials.findByLeadIds(ids).stream()
                .collect(Collectors.groupingBy(TrialBookingListItemDto::leadId));
        Map<LeadStatus, List<LeadOutput>> result = new EnumMap<>(LeadStatus.class);
        columns.forEach((status, items) -> result.put(status, items.stream()
                .map(l -> enrich(l, byLead.getOrDefault(l.id(), List.of()))).toList()));
        return new LeadKanbanOutput(result);
    }
    @Transactional(readOnly = true)
    public LeadOutput detail(UUID actor, UUID id) {
        check(actor, leads.getLeadBranchId(id));
        return enrich(leads.getLeadOutput(id, actor), trials.findByLeadIds(List.of(id)));
    }
    @Transactional
    public LeadOutput updateInput(UUID actor, UUID id, AdminLeadWorkInput input) {
        return update(actor, id, input.toCommand());
    }
    @Transactional
    public LeadOutput update(UUID actor, UUID id, LeadWorkCommand command) {
        check(actor, leads.getLeadBranchId(id));
        work.update(id, actor, command);
        return detail(actor, id);
    }
    private void check(UUID actor, UUID branchId) {
        if (!access.verifyAdminBelongsToBranch(actor, branchId))
            throw new AccessDeniedException("Нет доступа к филиалу");
    }
    // Repository order is newest first. Keep an active booking ahead of historical ones per participant.
    static LeadOutput enrich(LeadOutput lead, List<TrialBookingListItemDto> bookings) {
        Map<UUID, TrialBookingListItemDto> latest = new LinkedHashMap<>();
        bookings.forEach(t -> latest.merge(t.participantId() != null ? t.participantId() : t.studentId(), t,
                (old, next) -> old.status().name().equals("SCHEDULED") ? old
                        : next.status().name().equals("SCHEDULED") ? next : old));
        return lead.toBuilder().currentTrials(List.copyOf(latest.values())).build();
    }
}
