package kz.edu.soccerhub.crm.application.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import kz.edu.soccerhub.common.dto.lead.LeadChildOutput;
import kz.edu.soccerhub.common.dto.lead.LeadOutput;
import kz.edu.soccerhub.common.dto.lead.LeadTrialOutput;
import kz.edu.soccerhub.crm.domain.model.Lead;
import kz.edu.soccerhub.crm.domain.model.LeadTrial;

import java.util.List;

public final class LeadMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private LeadMapper() {
    }

    public static LeadOutput toOutput(Lead lead) {
        return new LeadOutput(
                lead.getId(),
                lead.getParentName(),
                lead.getPhone(),
                lead.getEmail(),
                lead.getSource(),
                lead.getStatus(),
                lead.getAssignedAdminId(),
                lead.getComment(),
                parseQualificationData(lead.getQualificationData()),
                mapChildren(lead),
                mapTrial(lead.getTrial()),
                lead.getCreatedAt(),
                lead.getUpdatedAt()
        );
    }

    private static List<LeadChildOutput> mapChildren(Lead lead) {
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

    private static LeadTrialOutput mapTrial(LeadTrial trial) {
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

    private static JsonNode parseQualificationData(String qualificationData) {
        if (qualificationData == null || qualificationData.isBlank()) {
            return NullNode.instance;
        }

        try {
            return OBJECT_MAPPER.readTree(qualificationData);
        } catch (Exception exception) {
            return OBJECT_MAPPER.valueToTree(qualificationData);
        }
    }
}

