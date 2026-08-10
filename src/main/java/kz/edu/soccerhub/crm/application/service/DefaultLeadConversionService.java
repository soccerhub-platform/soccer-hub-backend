package kz.edu.soccerhub.crm.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.edu.soccerhub.client.domain.enums.ClientSource;
import kz.edu.soccerhub.common.dto.client.ClientConversionCommand;
import kz.edu.soccerhub.common.dto.client.ClientConversionOutput;
import kz.edu.soccerhub.common.dto.lead.ConvertLeadRequest;
import kz.edu.soccerhub.common.dto.lead.ConvertLeadResponse;
import kz.edu.soccerhub.common.dto.lead.LeadConversionMode;
import kz.edu.soccerhub.common.dto.trial.LinkTrialStudentCommand;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.ConflictException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.ClientPort;
import kz.edu.soccerhub.common.port.TrialPort;
import kz.edu.soccerhub.crm.application.state.LeadEvent;
import kz.edu.soccerhub.crm.application.state.LeadStateMachineService;
import kz.edu.soccerhub.crm.domain.model.Lead;
import kz.edu.soccerhub.crm.domain.model.LeadParticipant;
import kz.edu.soccerhub.crm.domain.model.enums.LeadSource;
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
 * Creates or links the client/player records required for contract preparation.
 * Contract activation, payments and enrollment are separate workflows.
 */
@Service
@RequiredArgsConstructor
public class DefaultLeadConversionService implements LeadConversionService {

    private final LeadRepository leadRepository;
    private final ClientPort clientPort;
    private final LeadActivityService leadActivityService;
    private final ObjectMapper objectMapper;
    private final TrialPort trialPort;
    private final LeadStateMachineService leadStateMachineService;

    @Override
    @Transactional
    public ConvertLeadResponse convertLeadToClient(
            UUID leadId,
            ConvertLeadRequest request,
            UUID currentAdminId
    ) {
        validateRequest(request);

        Lead lead = leadRepository.findById(leadId)
                .orElseThrow(() -> new NotFoundException(
                        "Lead not found",
                        Map.of("leadId", leadId)
                ));

        validateConversionPath(lead, request.conversionMode());

        LeadParticipant participant = lead.getParticipants().stream()
                .filter(item -> Objects.equals(item.getId(), request.participantId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Participant does not belong to lead",
                        Map.of(
                                "leadId", leadId,
                                "participantId", request.participantId()
                        )
                ));

        validateParticipantReadyForConversion(participant);

        UUID existingClientId = resolveExistingClientId(
                lead,
                request.existingClientId()
        );

        ClientConversionOutput conversion = clientPort.convertLead(
                ClientConversionCommand.builder()
                        .existingClientId(existingClientId)
                        .primaryContactName(lead.getPrimaryContactName())
                        .phone(lead.getPrimaryContactPhone())
                        .email(lead.getPrimaryContactEmail())
                        .branchId(lead.getBranchId())
                        .source(toClientSource(lead.getSource()))
                        .comments(lead.getComment())
                        .participantName(participant.getFullName())
                        .participantBirthDate(request.participantBirthDate())
                        .relationshipType(request.relationshipType())
                        .replacePrimaryContact(request.replacePrimaryContact())
                        .replacePrimaryPayer(request.replacePrimaryPayer())
                        .sourceLeadId(leadId)
                        .actorUserId(currentAdminId)
                        .build()
        );

        LeadStatus previousStatus = lead.getStatus();

        lead.linkClient(conversion.clientId());
        participant.linkPlayer(conversion.playerId());

        LeadStatus nextStatus = leadStateMachineService.process(
                lead.getId(),
                previousStatus,
                LeadEvent.START_CONTRACT
        );
        lead.updateStatus(nextStatus);

        leadRepository.save(lead);

        if (request.conversionMode() == LeadConversionMode.AFTER_TRIAL) {
            trialPort.linkConvertedStudent(
                    LinkTrialStudentCommand.builder()
                            .leadId(leadId)
                            .participantId(participant.getId())
                            .clientId(conversion.clientId())
                            .studentId(conversion.playerId())
                            .build()
            );
        }

        leadActivityService.logStatusChanged(
                lead,
                LeadEvent.START_CONTRACT,
                previousStatus,
                currentAdminId,
                buildConversionDetails(request, conversion)
        );

        return new ConvertLeadResponse(
                lead.getId(),
                lead.getStatus(),
                conversion.clientId(),
                lead.getPrimaryContactName(),
                conversion.playerId(),
                participant.getFullName(),
                "CONTRACT_PENDING"
        );
    }

    private void validateRequest(ConvertLeadRequest request) {
        if (request == null
                || request.participantId() == null
                || request.participantBirthDate() == null
                || request.relationshipType() == null
                || request.conversionMode() == null) {
            throw new BadRequestException(
                    "participantId, participantBirthDate, relationshipType and conversionMode are required"
            );
        }
    }

    private void validateConversionPath(
            Lead lead,
            LeadConversionMode conversionMode
    ) {
        LeadStatus requiredStatus = switch (conversionMode) {
            case AFTER_TRIAL -> LeadStatus.DECISION_PENDING;
            case WITHOUT_TRIAL -> LeadStatus.IN_PROGRESS;
        };

        if (lead.getStatus() != requiredStatus) {
            throw new ConflictException(
                    "Lead cannot be converted using the requested path",
                    "LEAD_CONVERSION_STATUS_CONFLICT",
                    Map.of(
                            "leadId", String.valueOf(lead.getId()),
                            "conversionMode", conversionMode.name(),
                            "currentStatus", lead.getStatus().name(),
                            "requiredStatus", requiredStatus.name()
                    )
            );
        }
    }

    private void validateParticipantReadyForConversion(
            LeadParticipant participant
    ) {
        if (participant.getPlayerId() != null) {
            throw new ConflictException(
                    "Lead participant is already linked to a player",
                    "LEAD_PARTICIPANT_ALREADY_CONVERTED",
                    Map.of(
                            "participantId", String.valueOf(participant.getId()),
                            "playerId", participant.getPlayerId()
                    )
            );
        }
    }

    private UUID resolveExistingClientId(
            Lead lead,
            UUID requestedClientId
    ) {
        if (lead.getClientId() != null
                && requestedClientId != null
                && !Objects.equals(lead.getClientId(), requestedClientId)) {
            throw new ConflictException(
                    "Lead is already linked to another client",
                    "LEAD_CLIENT_CONFLICT",
                    Map.of(
                            "leadId", String.valueOf(lead.getId()),
                            "currentClientId", lead.getClientId(),
                            "requestedClientId", requestedClientId
                    )
            );
        }

        return lead.getClientId() != null
                ? lead.getClientId()
                : requestedClientId;
    }

    private String buildConversionDetails(
            ConvertLeadRequest request,
            ClientConversionOutput conversion
    ) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("participantId", request.participantId());
            payload.put("playerId", conversion.playerId());
            payload.put("clientId", conversion.clientId());
            payload.put("relationId", conversion.relationId());
            payload.put("relationshipType", request.relationshipType());
            payload.put("conversionMode", request.conversionMode());
            payload.put("requestedExistingClientId", request.existingClientId());
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            return "Player created for contract preparation: "
                    + conversion.playerId();
        }
    }

    private ClientSource toClientSource(LeadSource source) {
        if (source == null) {
            return null;
        }

        return switch (source) {
            case WHATSAPP -> ClientSource.WHATSAPP;
            case INSTAGRAM -> ClientSource.INSTAGRAM;
            case CALL -> ClientSource.PHONE;
            case WEBSITE -> ClientSource.WEBSITE;
            case WALK_IN -> ClientSource.WALK_IN;
            case OTHER -> ClientSource.OTHER;
        };
    }
}