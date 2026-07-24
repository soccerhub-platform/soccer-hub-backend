package kz.edu.soccerhub.crm.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.edu.soccerhub.common.dto.client.ClientConversionCommand;
import kz.edu.soccerhub.common.dto.client.ClientConversionOutput;
import kz.edu.soccerhub.common.dto.lead.ConvertLeadRequest;
import kz.edu.soccerhub.common.dto.lead.ConvertLeadResponse;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.ClientPort;
import kz.edu.soccerhub.crm.domain.model.Lead;
import kz.edu.soccerhub.crm.domain.model.LeadParticipant;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.crm.domain.repository.LeadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Converts a sales lead into CRM roles only.
 * Contracts, payments and group enrollment are separate workflows.
 */
@Service
@RequiredArgsConstructor
public class DefaultLeadConversionService implements LeadConversionService {

    private final LeadRepository leadRepository;
    private final ClientPort clientPort;
    private final LeadActivityService leadActivityService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ConvertLeadResponse convertLeadToClient(UUID leadId, ConvertLeadRequest request, UUID currentAdminId) {
        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new NotFoundException("Lead not found", Map.of("leadId", leadId)));
        validateRequest(request);
        validateStatus(lead);

        LeadParticipant participant = lead.getParticipants().stream()
                .filter(item -> Objects.equals(item.getId(), request.participantId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Participant does not belong to lead",
                        Map.of("leadId", leadId, "participantId", request.participantId())
                ));

        ClientConversionOutput conversion = clientPort.convertLead(new ClientConversionCommand(
                lead.getClientId(),
                lead.getPrimaryContactName(),
                lead.getPrimaryContactPhone(),
                lead.getPrimaryContactEmail(),
                lead.getBranchId(),
                lead.getSource() == null ? null : lead.getSource().name(),
                lead.getComment(),
                participant.getFullName(),
                request.participantBirthDate(),
                request.relationshipType(),
                request.replacePrimaryContact(),
                request.replacePrimaryPayer()
        ));

        LeadStatus previousStatus = lead.getStatus();
        lead.markConverted(conversion.clientId(), conversion.playerId());
        leadRepository.save(lead);
        leadActivityService.logLeadConverted(
                lead,
                currentAdminId,
                previousStatus,
                buildConversionDetails(request, conversion)
        );

        return new ConvertLeadResponse(
                lead.getId(),
                lead.getStatus(),
                conversion.clientId(),
                lead.getPrimaryContactName(),
                conversion.playerId(),
                participant.getFullName(),
                "CONVERTED"
        );
    }

    private void validateRequest(ConvertLeadRequest request) {
        if (request == null || request.participantId() == null
                || request.participantBirthDate() == null || request.relationshipType() == null) {
            throw new BadRequestException("participantId, participantBirthDate and relationshipType are required");
        }
    }

    private void validateStatus(Lead lead) {
        if (lead.getStatus() == LeadStatus.CONVERTED || lead.getStatus() == LeadStatus.LOST) {
            throw new BadRequestException("Lead is already closed", lead.getStatus());
        }
    }

    private String buildConversionDetails(ConvertLeadRequest request, ClientConversionOutput conversion) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("participantId", request.participantId());
            payload.put("playerId", conversion.playerId());
            payload.put("clientId", conversion.clientId());
            payload.put("relationId", conversion.relationId());
            payload.put("relationshipType", request.relationshipType());
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "Lead converted to client " + conversion.clientId();
        }
    }
}
