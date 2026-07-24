package kz.edu.soccerhub.crm.application.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.dto.lead.*;
import kz.edu.soccerhub.common.dto.coach.CoachDto;
import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.common.port.AdminPort;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.crm.application.resolver.LeadActionResolver;
import kz.edu.soccerhub.crm.domain.model.Lead;
import kz.edu.soccerhub.crm.domain.model.LeadTrial;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class LeadMapper {

    private final AdminPort adminPort;
    private final GroupPort groupPort;
    private final CoachPort coachPort;
    private final LeadActionResolver leadActionResolver;
    private final ObjectMapper objectMapper;

    public LeadOutput toOutput(Lead lead, UUID currentAdminId) {
        return toOutputs(List.of(lead), currentAdminId).getFirst();
    }

    public List<LeadOutput> toOutputs(Collection<Lead> leads, UUID currentAdminId) {
        LeadReadContext context = buildContext(leads);
        return leads.stream()
                .map(lead -> toOutput(lead, currentAdminId, context))
                .toList();
    }

    private LeadOutput toOutput(Lead lead, UUID currentAdminId, LeadReadContext context) {
        String groupName = resolveGroupName(lead, context);
        String coachName = resolveCoachName(lead, context);

        return new LeadOutput(
                lead.getId(),
                lead.getLeadType(),
                new LeadPrimaryContactOutput(
                        lead.getPrimaryContactName(),
                        lead.getPrimaryContactPhone(),
                        lead.getPrimaryContactEmail()
                ),
                lead.getSource(),
                lead.getStatus(),
                leadActionResolver.resolve(lead, currentAdminId),
                mapAssignedAdmin(lead.getAssignedAdminId()),
                lead.getComment(),
                parseQualificationData(lead.getQualificationData()),
                lead.getPreferredDays(),
                lead.getTimePreference(),
                lead.getExperience(),
                lead.getNotes(),
                lead.getLostReasonCode(),
                lead.getLostReason() == null ? null : lead.getLostReason().getName(),
                lead.getLostComment(),
                lead.getLostAt(),
                lead.getClientId(),
                lead.getParticipantId(),
                groupName,
                coachName,
                mapParticipants(lead),
                mapTrial(lead.getTrial(), context),
                lead.getCreatedAt(),
                lead.getUpdatedAt()
        );
    }

    private List<LeadParticipantOutput> mapParticipants(Lead lead) {
        return lead.getParticipants().stream()
                .map(participant -> new LeadParticipantOutput(
                        participant.getId(),
                        participant.getFullName(),
                        participant.getBirthDate(),
                        participant.getGender(),
                        participant.getExperience()
                ))
                .toList();
    }

    private LeadTrialOutput mapTrial(LeadTrial trial, LeadReadContext context) {
        if (trial == null) {
            return null;
        }

        return new LeadTrialOutput(
                trial.getId(),
                trial.getLead().getId(),
                trial.getParticipantId(),
                trial.getGroupId(),
                trial.getGroupId() == null ? null : mapGroupName(trial.getGroupId(), context.groupsById()),
                trial.getCoachId(),
                trial.getCoachId() == null ? null : mapCoachName(trial.getCoachId(), context.coachesById()),
                trial.getTrialDate(),
                trial.getStartTime(),
                trial.getEndTime(),
                trial.getComment(),
                trial.getStatus()
        );
    }

    private LeadReadContext buildContext(Collection<Lead> leads) {
        if (leads == null || leads.isEmpty()) {
            return LeadReadContext.empty();
        }

        Set<UUID> groupIds = leads.stream()
                .map(lead -> lead.getTrial() == null ? null : lead.getTrial().getGroupId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, GroupDto> groupsById = groupIds.isEmpty()
                ? Map.of()
                : groupPort.getGroupsByIds(groupIds).stream().collect(Collectors.toMap(GroupDto::groupId, item -> item));

        Set<UUID> coachIds = leads.stream()
                .map(lead -> lead.getTrial() == null ? null : lead.getTrial().getCoachId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, CoachDto> coachesById = coachIds.isEmpty()
                ? Map.of()
                : coachPort.getCoaches(coachIds).stream().collect(Collectors.toMap(CoachDto::id, item -> item));

        return new LeadReadContext(groupsById, coachesById);
    }

    private String resolveGroupName(Lead lead, LeadReadContext context) {
        return lead.getTrial() == null || lead.getTrial().getGroupId() == null
                ? null
                : mapGroupName(lead.getTrial().getGroupId(), context.groupsById());
    }

    private String resolveCoachName(Lead lead, LeadReadContext context) {
        return lead.getTrial() == null || lead.getTrial().getCoachId() == null
                ? null
                : mapCoachName(lead.getTrial().getCoachId(), context.coachesById());
    }

    private String mapGroupName(UUID groupId, Map<UUID, GroupDto> groupsById) {
        GroupDto group = groupsById.get(groupId);
        return group == null ? null : group.name();
    }

    private String mapCoachName(UUID coachId, Map<UUID, CoachDto> coachesById) {
        CoachDto coach = coachesById.get(coachId);
        if (coach == null) {
            return null;
        }
        String fullName = ((coach.firstName() == null ? "" : coach.firstName()) + " "
                + (coach.lastName() == null ? "" : coach.lastName())).trim();
        return fullName.isBlank() ? null : fullName;
    }

    private AdminShortOutput mapAssignedAdmin(UUID assignedAdminId) {
        if (assignedAdminId == null) {
            return null;
        }

        return adminPort.findById(assignedAdminId)
                .map(this::toAdminShortOutput)
                .orElse(null);
    }

    private AdminShortOutput toAdminShortOutput(AdminDto adminDto) {
        String fullName = ((adminDto.firstName() == null ? "" : adminDto.firstName()) + " "
                + (adminDto.lastName() == null ? "" : adminDto.lastName())).trim();

        return new AdminShortOutput(
                adminDto.id(),
                fullName.isBlank() ? null : fullName,
                adminDto.email()
        );
    }

    private JsonNode parseQualificationData(String qualificationData) {
        if (qualificationData == null || qualificationData.isBlank()) {
            return NullNode.instance;
        }

        try {
            return objectMapper.readTree(qualificationData);
        } catch (Exception exception) {
            return objectMapper.valueToTree(qualificationData);
        }
    }

    private record LeadReadContext(
            Map<UUID, GroupDto> groupsById,
            Map<UUID, CoachDto> coachesById
    ) {
        private static LeadReadContext empty() {
            return new LeadReadContext(
                    Collections.emptyMap(),
                    Collections.emptyMap()
            );
        }
    }
}
