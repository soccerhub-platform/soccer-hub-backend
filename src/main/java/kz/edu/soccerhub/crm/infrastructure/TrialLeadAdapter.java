package kz.edu.soccerhub.crm.infrastructure;

import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;
import kz.edu.soccerhub.common.port.TrialLeadPort;
import kz.edu.soccerhub.crm.domain.model.Lead;
import kz.edu.soccerhub.crm.domain.repository.LeadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TrialLeadAdapter implements TrialLeadPort {

    private final LeadRepository repository;

    @Override
    public void validateParticipant(UUID leadId, UUID participantId) {
        Lead lead = repository.findById(leadId)
                .orElseThrow(() -> new NotFoundException("Lead not found", leadId));

        boolean belongsToLead = lead.getParticipants().stream()
                .anyMatch(participant -> participantId.equals(participant.getId()));

        if (!belongsToLead) {
            throw new BadRequestException(
                    "Participant does not belong to lead",
                    leadId,
                    participantId
            );
        }
    }

    @Override
    public void validateConvertedStudent(UUID leadId, UUID studentId) {
        Lead lead = repository.findById(leadId)
                .orElseThrow(() -> new NotFoundException("Lead not found", leadId));

        if (!studentId.equals(lead.getParticipantId())) {
            throw new BadRequestException("Student does not belong to lead", leadId, studentId);
        }
    }

    @Override
    public TrialBookingDetailsDto.Student getParticipantDetails(UUID leadId, UUID participantId) {
        Lead lead = repository.findById(leadId)
                .orElseThrow(() -> new NotFoundException("Lead not found", leadId));

        return lead.getParticipants().stream()
                .filter(participant -> participantId.equals(participant.getId()))
                .findFirst()
                .map(participant -> TrialBookingDetailsDto.Student.builder()
                        .id(participant.getId())
                        .fullName(participant.getFullName())
                        .birthDate(participant.getBirthDate())
                        .build())
                .orElseThrow(() -> new NotFoundException("Lead participant not found", participantId));
    }

    @Override
    public TrialBookingDetailsDto.Lead getDetails(UUID leadId) {
        Lead lead = repository.findById(leadId)
                .orElseThrow(() -> new NotFoundException("Lead not found", leadId));

        return TrialBookingDetailsDto.Lead.builder()
                .id(lead.getId())
                .fullName(lead.getPrimaryContactName())
                .phone(lead.getPrimaryContactPhone())
                .email(lead.getPrimaryContactEmail())
                .build();
    }

    @Override
    public Map<UUID, TrialBookingDetailsDto.Lead> getDetails(
            Collection<UUID> leadIds
    ) {
        if (leadIds.isEmpty()) {
            return Map.of();
        }

        return repository.findAllById(leadIds).stream()
                .collect(Collectors.toMap(
                        Lead::getId,
                        lead -> TrialBookingDetailsDto.Lead.builder()
                                .id(lead.getId())
                                .fullName(lead.getPrimaryContactName())
                                .phone(lead.getPrimaryContactPhone())
                                .email(lead.getPrimaryContactEmail())
                                .build()
                ));
    }

    @Override
    public Map<UUID, TrialBookingDetailsDto.Student> getParticipantDetails(
            Collection<UUID> participantIds
    ) {
        if (participantIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, TrialBookingDetailsDto.Student> result = new HashMap<>();

        repository.findByParticipantIdInOrderByUpdatedAtDesc(participantIds)
                .forEach(lead -> lead.getParticipants().stream()
                        .filter(participant -> participantIds.contains(participant.getId()))
                        .forEach(participant -> result.putIfAbsent(
                                participant.getId(),
                                TrialBookingDetailsDto.Student.builder()
                                        .id(participant.getId())
                                        .fullName(participant.getFullName())
                                        .birthDate(participant.getBirthDate())
                                        .build()
                        )));

        return result;
    }
}
