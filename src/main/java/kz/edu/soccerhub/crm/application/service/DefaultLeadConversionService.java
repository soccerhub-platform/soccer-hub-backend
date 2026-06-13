package kz.edu.soccerhub.crm.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.edu.soccerhub.common.dto.client.ClientConversionCommand;
import kz.edu.soccerhub.common.dto.client.ClientConversionOutput;
import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.common.dto.lead.ConvertLeadRequest;
import kz.edu.soccerhub.common.dto.lead.ConvertLeadResponse;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.ClientPort;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.crm.domain.model.Lead;
import kz.edu.soccerhub.crm.domain.model.LeadParticipant;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.crm.domain.repository.LeadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultLeadConversionService implements LeadConversionService {

    private final LeadRepository leadRepository;
    private final GroupPort groupPort;
    private final ClientPort clientPort;
    private final LeadActivityService leadActivityService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ConvertLeadResponse convertLeadToClient(UUID leadId, ConvertLeadRequest request, UUID currentAdminId) {
        Lead lead = findById(leadId);
        validateConvertRequest(request);
        validateLeadStatusForConversion(lead);

        LeadParticipant participant = lead.getParticipants().stream()
                .filter(item -> Objects.equals(item.getId(), request.participantId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Participant does not belong to lead",
                        Map.of("leadId", leadId, "participantId", request.participantId())
                ));

        GroupDto group = groupPort.getGroupById(request.groupId());
        if (!Objects.equals(group.branchId(), lead.getBranchId())) {
            throw new BadRequestException(
                    "Group does not belong to lead branch",
                    Map.of("leadBranchId", lead.getBranchId(), "groupBranchId", group.branchId())
            );
        }

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
                request.groupId(),
                request.contractStartDate(),
                request.contractEndDate(),
                request.amount()
        ));

        LeadStatus previousStatus = lead.getStatus();
        if (lead.getStatus() != LeadStatus.WON) {
            lead.updateStatus(LeadStatus.WON);
        }
        lead.markConverted(conversion.clientId(), conversion.playerId(), conversion.contractId());
        leadRepository.save(lead);

        leadActivityService.logLeadConverted(
                lead,
                currentAdminId,
                previousStatus,
                buildConversionDetails(request, conversion.playerId(), conversion.contractId())
        );

        return new ConvertLeadResponse(
                lead.getId(),
                conversion.clientId(),
                conversion.playerId(),
                conversion.contractId(),
                "CONVERTED"
        );
    }

    private Lead findById(UUID leadId) {
        return leadRepository.findById(leadId)
                .orElseThrow(() -> new NotFoundException("Lead not found", Map.of("leadId", leadId)));
    }

    private void validateConvertRequest(ConvertLeadRequest request) {
        if (request == null) {
            throw new BadRequestException("Conversion payload is required");
        }
        if (request.participantId() == null) {
            throw new BadRequestException("participantId is required");
        }
        if (request.groupId() == null) {
            throw new BadRequestException("groupId is required");
        }
        if (request.participantBirthDate() == null) {
            throw new BadRequestException("participantBirthDate is required");
        }
        if (request.contractStartDate() == null) {
            throw new BadRequestException("contractStartDate is required");
        }
        if (request.contractEndDate() != null && request.contractEndDate().isBefore(request.contractStartDate())) {
            throw new BadRequestException("contractEndDate must be after contractStartDate");
        }
        if (request.amount() != null && request.amount().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("amount must be >= 0");
        }
    }

    private void validateLeadStatusForConversion(Lead lead) {
        boolean allowed = lead.getStatus() == LeadStatus.TRIAL_DONE
                          || lead.getStatus() == LeadStatus.QUALIFIED
                          || lead.getStatus() == LeadStatus.TRIAL_SCHEDULED
                          || lead.getStatus() == LeadStatus.WAITING_PAYMENT
                          || lead.getStatus() == LeadStatus.WON;
        if (!allowed) {
            throw new BadRequestException("Lead status is not allowed for conversion", lead.getStatus());
        }
    }

    private String buildConversionDetails(ConvertLeadRequest request, UUID playerId, UUID contractId) {
        try {
            Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("participantId", request.participantId());
            payload.put("groupId", request.groupId());
            payload.put("playerId", playerId);
            payload.put("contractId", contractId);
            payload.put("amount", request.amount());
            payload.put("contractStartDate", request.contractStartDate() == null ? null : request.contractStartDate().toString());
            payload.put("contractEndDate", request.contractEndDate() == null ? null : request.contractEndDate().toString());
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "Lead converted with contract " + contractId;
        }
    }
}
