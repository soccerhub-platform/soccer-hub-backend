package kz.edu.soccerhub.crm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import kz.edu.soccerhub.common.dto.lead.LeadCreateCommand;
import kz.edu.soccerhub.common.dto.lead.LeadChildInput;
import kz.edu.soccerhub.common.dto.lead.LeadOutput;
import kz.edu.soccerhub.common.dto.lead.LeadQualificationInput;
import kz.edu.soccerhub.common.dto.lead.ScheduleTrialInput;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.AdminPort;
import kz.edu.soccerhub.common.port.ClientPort;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.common.port.LeadPort;
import kz.edu.soccerhub.crm.application.mapper.LeadMapper;
import kz.edu.soccerhub.crm.domain.model.Lead;
import kz.edu.soccerhub.crm.domain.model.enums.LeadSource;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.crm.domain.repository.LeadRepository;
import kz.edu.soccerhub.crm.infrastructure.LeadSpecification;
import kz.edu.soccerhub.crm.state.LeadEvent;
import kz.edu.soccerhub.crm.state.LeadStateMachineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeadService implements LeadPort {

    private final LeadRepository leadRepository;
    private final LeadStateMachineService stateMachineService;
    private final AdminPort adminPort;
    private final ClientPort clientPort;
    private final GroupPort groupPort;
    private final CoachPort coachPort;
    private final LeadActivityService leadActivityService;
    private final ObjectMapper objectMapper;

    @Transactional
    @Override
    public UUID createLead(@Valid LeadCreateCommand command) {
        validateAssignedAdmin(command.assignedAdminId());

        String normalizedPhone = PhoneNormalizer.normalize(command.phone());
        ensureNoActiveDuplicate(normalizedPhone);

        Lead lead = Lead.builder()
                .id(UUID.randomUUID())
                .parentName(trim(command.parentName()))
                .phone(normalizedPhone)
                .email(trim(command.email()))
                .childName(trim(command.childName()))
                .childAge(command.childAge())
                .source(LeadSource.OTHER)
                .status(LeadStatus.NEW)
                .assignedAdminId(command.assignedAdminId())
                .branchId(command.branchId())
                .comment(trim(command.comment()))
                .build();

        addChildren(lead, command);

        leadRepository.save(lead);
        leadActivityService.logLeadCreated(lead);
        return lead.getId();
    }

    @Override
    @Transactional
    public void qualifyLead(UUID leadId, @Valid LeadQualificationInput input) {
        Lead lead = findById(leadId);
        LeadStatus previousStatus = lead.getStatus();

        String qualificationData = serializeQualificationData(input);
        LeadStatus newStatus = stateMachineService.process(leadId, previousStatus, LeadEvent.QUALIFY);

        lead.updateQualificationData(qualificationData);
        lead.updateQualificationFields(
                trim(input.preferredDays()),
                trim(input.experience()),
                trim(input.notes())
        );
        replaceChildrenFromQualification(lead, input.children());
        lead.updateStatus(newStatus);

        leadRepository.save(lead);
        leadActivityService.logStatusChanged(lead, LeadEvent.QUALIFY, previousStatus);

        log.info("Lead {} qualified. status: {} -> {}", leadId, previousStatus, newStatus);
    }

    @Override
    @Transactional
    public void scheduleTrial(UUID leadId, @Valid ScheduleTrialInput input) {
        validateTrialInput(input);

        Lead lead = findById(leadId);
        validateChildBelongsToLead(lead, input.childId());
        validateGroupAndCoach(lead, input.groupId(), input.coachId());

        LeadStatus previousStatus = lead.getStatus();

        LocalDateTime trialDateTime = LocalDateTime.of(input.trialDate(), input.startTime());
        lead.scheduleTrial(
                input.childId(),
                input.groupId(),
                input.coachId(),
                trialDateTime,
                input.durationMinutes(),
                trim(input.comment())
        );
        LeadStatus newStatus = stateMachineService.process(leadId, previousStatus, LeadEvent.SCHEDULE_TRIAL);
        lead.updateStatus(newStatus);

        leadRepository.save(lead);
        leadActivityService.logStatusChanged(lead, LeadEvent.SCHEDULE_TRIAL, previousStatus);
        log.info("Trial scheduled for lead {}", leadId);
    }

    @Transactional(readOnly = true)
    public Page<Lead> getLeads(
            List<LeadStatus> statuses,
            UUID assignedAdminId,
            UUID branchId,
            Boolean unassigned,
            String search,
            LocalDate createdFrom,
            LocalDate createdTo,
            Pageable pageable
    ) {
        var specification = LeadSpecification.build(
                statuses,
                assignedAdminId,
                branchId,
                unassigned,
                search,
                createdFrom,
                createdTo
        );
        return leadRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    @Override
    public Map<LeadStatus, List<LeadOutput>> getKanban(UUID branchId) {
        var specification = LeadSpecification.build(
                null,
                null,
                branchId,
                null,
                null,
                null,
                null
        );

        List<Lead> branchLeads = leadRepository.findAll(specification);
        List<Lead> filteredLeads = branchLeads;

        Map<LeadStatus, List<Lead>> leadColumns = new EnumMap<>(LeadStatus.class);
        for (LeadStatus status : LeadStatus.values()) {
            leadColumns.put(status, new ArrayList<>());
        }

        for (Lead lead : filteredLeads) {
            leadColumns.get(lead.getStatus()).add(lead);
        }

        leadColumns.values().forEach(leads -> leads.sort(
                Comparator.comparing(Lead::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
        ));

        Map<LeadStatus, List<LeadOutput>> outputColumns = new EnumMap<>(LeadStatus.class);
        for (LeadStatus status : LeadStatus.values()) {
            outputColumns.put(status, leadColumns.get(status).stream().map(LeadMapper::toOutput).toList());
        }

        return outputColumns;
    }

    @Transactional(readOnly = true)
    public Lead getLeadById(UUID leadId) {
        return findById(leadId);
    }

    @Transactional
    public void assignLead(UUID leadId, UUID assignedAdminId) {
        validateAssignedAdmin(assignedAdminId);

        Lead lead = findById(leadId);
        UUID previousAdminId = lead.getAssignedAdminId();
        lead.assignAdmin(assignedAdminId);

        leadRepository.save(lead);
        leadActivityService.logLeadAssigned(lead, previousAdminId);
    }

    @Transactional
    @Override
    public LeadStatus processEvent(UUID leadId, LeadEvent event) {
        Lead lead = findById(leadId);
        LeadStatus previousStatus = lead.getStatus();

        LeadStatus newStatus = stateMachineService.process(lead.getId(), previousStatus, event);
        lead.updateStatus(newStatus);

        leadRepository.save(lead);
        leadActivityService.logStatusChanged(lead, event, previousStatus);

        return newStatus;
    }

    @Transactional
    @Override
    public UUID convertLeadToClient(UUID leadId) {
        Lead lead = findById(leadId);
        if (lead.getStatus() != LeadStatus.WON) {
            throw new BadRequestException("Only WON leads can be converted to client", leadId);
        }
        if (lead.getClientId() != null) {
            return lead.getClientId();
        }

        ensureChildrenFromLegacyForConversion(lead);
        if (!lead.isReadyForConversion()) {
            throw new BadRequestException("Lead is not ready for conversion", leadId);
        }

        UUID clientId = clientPort.createClient(
                lead.getParentName(),
                lead.getPhone(),
                lead.getEmail()
        );

        List<LeadChildInput> children = resolveChildrenForConversion(lead);
        if (children.isEmpty()) {
            throw new BadRequestException("At least one child is required to convert lead", leadId);
        }

        for (LeadChildInput child : children) {
            clientPort.createPlayer(
                    clientId,
                    child.childName(),
                    child.childAge()
            );
        }

        lead.markConverted(clientId);
        leadRepository.save(lead);
        leadActivityService.logLeadConverted(lead);

        log.info("Lead {} converted to client {}", leadId, clientId);

        return clientId;
    }

    private void addChildren(Lead lead, LeadCreateCommand command) {
        if (command.children() != null && !command.children().isEmpty()) {
            for (LeadChildInput child : command.children()) {
                lead.addChild(trim(child.childName()), child.childAge());
            }
            return;
        }

        if (command.childName() != null && !command.childName().isBlank()) {
            lead.addChild(trim(command.childName()), command.childAge());
        }
    }

    private List<LeadChildInput> resolveChildrenForConversion(Lead lead) {
        List<LeadChildInput> leadChildren = lead.getChildren().stream()
                .map(child -> new LeadChildInput(trim(child.getChildName()), child.getChildAge()))
                .toList();

        if (!leadChildren.isEmpty()) {
            return leadChildren;
        }

        String legacyChildName = trim(lead.getChildName());
        if (legacyChildName == null || legacyChildName.isBlank()) {
            return List.of();
        }

        return List.of(new LeadChildInput(legacyChildName, lead.getChildAge()));
    }

    private void replaceChildrenFromQualification(Lead lead, List<LeadChildInput> children) {
        if (children == null) {
            return;
        }

        lead.clearChildren();
        for (LeadChildInput child : children) {
            lead.addChild(trim(child.childName()), child.childAge());
        }
    }

    private void ensureChildrenFromLegacyForConversion(Lead lead) {
        if (!lead.getChildren().isEmpty()) {
            return;
        }

        String legacyChildName = trim(lead.getChildName());
        if (legacyChildName == null || legacyChildName.isBlank()) {
            return;
        }

        lead.addChild(legacyChildName, lead.getChildAge());
    }

    private void ensureNoActiveDuplicate(String normalizedPhone) {
        boolean exists = leadRepository.existsActiveLeadByPhone(normalizedPhone);

        if (exists) {
            throw new BadRequestException("Lead with this phone already exists in active pipeline", normalizedPhone);
        }
    }

    private void validateAssignedAdmin(UUID assignedAdminId) {
        if (assignedAdminId == null) {
            return;
        }

        if (!adminPort.verifyAdmin(assignedAdminId)) {
            throw new NotFoundException("Assigned admin not found", Map.of("adminId", assignedAdminId));
        }
    }

    private Lead findById(UUID leadId) {
        return leadRepository.findById(leadId)
                .orElseThrow(() -> new NotFoundException("Lead not found", Map.of("leadId", leadId)));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String serializeQualificationData(LeadQualificationInput input) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(input);
        } catch (JsonProcessingException exception) {
            throw new BadRequestException("Invalid qualification data payload", exception.getMessage());
        }
    }

    private void validateTrialInput(ScheduleTrialInput input) {
        if (input == null) {
            throw new BadRequestException("Trial payload is required");
        }
        if (input.childId() == null) {
            throw new BadRequestException("Child id is required");
        }
        if (input.groupId() == null && input.coachId() == null) {
            throw new BadRequestException("Either group id or coach id is required");
        }
        if (input.trialDate() == null) {
            throw new BadRequestException("Trial date is required");
        }
        if (input.startTime() == null) {
            throw new BadRequestException("Start time is required");
        }
        if (input.durationMinutes() != null && input.durationMinutes() <= 0) {
            throw new BadRequestException("Duration minutes must be greater than 0");
        }
    }

    private void validateChildBelongsToLead(Lead lead, UUID childId) {
        boolean exists = lead.getChildren().stream()
                .anyMatch(child -> child.getId().equals(childId));

        if (!exists) {
            throw new BadRequestException("Child does not belong to lead", Map.of("leadId", lead.getId(), "childId", childId));
        }
    }

    private void validateGroupAndCoach(Lead lead, UUID groupId, UUID coachId) {
        if (groupId != null) {
            var group = groupPort.getGroupById(groupId);
            if (!lead.getBranchId().equals(group.branchId())) {
                throw new BadRequestException("Group does not belong to lead branch", Map.of("branchId", lead.getBranchId(), "groupId", groupId));
            }
        }

        if (coachId != null && !coachPort.verifyCoach(coachId)) {
            throw new NotFoundException("Coach not found", Map.of("coachId", coachId));
        }
    }

}
