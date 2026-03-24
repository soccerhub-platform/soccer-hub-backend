package kz.edu.soccerhub.crm.application.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import kz.edu.soccerhub.common.dto.lead.LeadChildInput;
import kz.edu.soccerhub.common.dto.lead.LeadOutput;
import kz.edu.soccerhub.crm.domain.model.Lead;

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
                lead.getChildName(),
                lead.getChildAge(),
                lead.getSource(),
                lead.getStatus(),
                lead.getAssignedAdminId(),
                lead.getComment(),
                parseQualificationData(lead.getQualificationData()),
                mapChildren(lead),
                lead.getCreatedAt(),
                lead.getUpdatedAt()
        );
    }

    private static List<LeadChildInput> mapChildren(Lead lead) {
        return lead.getChildren().stream()
                .map(child -> new LeadChildInput(child.getChildName(), child.getChildAge()))
                .toList();
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

