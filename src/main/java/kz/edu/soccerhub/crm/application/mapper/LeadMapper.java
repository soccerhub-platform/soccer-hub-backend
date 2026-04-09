package kz.edu.soccerhub.crm.application.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.dto.lead.AdminShortOutput;
import kz.edu.soccerhub.common.dto.lead.LeadChildOutput;
import kz.edu.soccerhub.common.dto.lead.LeadOutput;
import kz.edu.soccerhub.common.dto.lead.LeadTrialOutput;
import kz.edu.soccerhub.common.port.AdminPort;
import kz.edu.soccerhub.crm.application.resolver.LeadActionResolver;
import kz.edu.soccerhub.crm.domain.model.Lead;
import kz.edu.soccerhub.crm.domain.model.LeadTrial;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LeadMapper {

    private final AdminPort adminPort;
    private final LeadActionResolver leadActionResolver;
    private final ObjectMapper objectMapper;

    public LeadOutput toOutput(Lead lead, UUID currentAdminId) {
        return new LeadOutput(
                lead.getId(),
                lead.getParentName(),
                lead.getPhone(),
                lead.getEmail(),
                lead.getSource(),
                lead.getStatus(),
                leadActionResolver.resolve(lead, currentAdminId),
                mapAssignedAdmin(lead.getAssignedAdminId()),
                lead.getComment(),
                parseQualificationData(lead.getQualificationData()),
                mapChildren(lead),
                mapTrial(lead.getTrial()),
                lead.getCreatedAt(),
                lead.getUpdatedAt()
        );
    }

    public LeadOutput toOutput(Lead lead) {
        return toOutput(lead, null);
    }

    private List<LeadChildOutput> mapChildren(Lead lead) {
        return lead.getChildren().stream()
                .map(child -> new LeadChildOutput(
                        child.getId(),
                        child.getChildName(),
                        child.getChildAge(),
                        child.getGender(),
                        child.getExperience()
                ))
                .toList();
    }

    private LeadTrialOutput mapTrial(LeadTrial trial) {
        if (trial == null) {
            return null;
        }

        return new LeadTrialOutput(
                trial.getId(),
                trial.getLead().getId(),
                trial.getChildId(),
                trial.getGroupId(),
                trial.getCoachId(),
                trial.getTrialDate(),
                trial.getStartTime(),
                trial.getEndTime(),
                trial.getComment(),
                trial.getStatus()
        );
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
}

