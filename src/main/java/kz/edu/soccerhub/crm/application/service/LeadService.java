package kz.edu.soccerhub.crm.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import kz.edu.soccerhub.common.dto.lead.LeadActivityOutput;
import kz.edu.soccerhub.common.dto.lead.LeadCreateCommand;
import kz.edu.soccerhub.common.dto.lead.LeadChildInput;
import kz.edu.soccerhub.common.dto.lead.LeadLossReasonResponse;
import kz.edu.soccerhub.common.dto.lead.LeadOutput;
import kz.edu.soccerhub.common.dto.lead.LeadQualificationInput;
import kz.edu.soccerhub.common.dto.lead.ScheduleTrialInput;
import kz.edu.soccerhub.common.dto.group.GroupScheduleDto;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.AdminPort;
import kz.edu.soccerhub.common.port.ClientPort;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.common.port.GroupSchedulePort;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.common.port.LeadPort;
import kz.edu.soccerhub.crm.application.mapper.LeadMapper;
import kz.edu.soccerhub.crm.domain.model.Lead;
import kz.edu.soccerhub.crm.domain.model.LeadLossReasonEntity;
import kz.edu.soccerhub.crm.domain.model.enums.LeadSource;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.crm.domain.repository.LeadLossReasonRepository;
import kz.edu.soccerhub.crm.domain.repository.LeadRepository;
import kz.edu.soccerhub.crm.infrastructure.LeadSpecification;
import kz.edu.soccerhub.organization.application.dto.ScheduleSearchCriteria;
import kz.edu.soccerhub.organization.domain.model.enums.ScheduleStatus;
import kz.edu.soccerhub.crm.application.state.LeadEvent;
import kz.edu.soccerhub.crm.application.state.LeadStateMachineService;
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
import java.util.Objects;
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
    private final GroupSchedulePort groupSchedulePort;
    private final LeadActivityService leadActivityService;
    private final LeadMapper leadMapper;
    private final LeadLossReasonRepository leadLossReasonRepository;
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
                .source(LeadSource.OTHER)
                .status(LeadStatus.NEW)
                .assignedAdminId(command.assignedAdminId())
                .branchId(command.branchId())
                .comment(trim(command.comment()))
                .build();

        for (LeadChildInput child : command.children()) {
            lead.addChild(
                    trim(child.childName()),
                    child.childAge(),
                    child.gender(),
                    trim(child.experience())
            );
        }

        leadRepository.save(lead);
        leadActivityService.logLeadCreated(lead);
        return lead.getId();
    }

    @Override
    @Transactional
    public void qualifyLead(UUID leadId, @Valid LeadQualificationInput input, UUID currentAdminId) {
        Lead lead = findById(leadId);

        if (lead.getAssignedAdminId() == null && currentAdminId != null) {
            validateAssignedAdmin(currentAdminId);
            lead.assignAdmin(currentAdminId);
        }

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
        leadActivityService.logStatusChanged(lead, LeadEvent.QUALIFY, previousStatus, currentAdminId);

        log.info("Lead {} qualified. status: {} -> {}", leadId, previousStatus, newStatus);
    }

    @Override
    @Transactional
    public void scheduleTrial(UUID leadId, @Valid ScheduleTrialInput input, UUID currentAdminId) {
        validateTrialInput(input);

        Lead lead = findById(leadId);
        validateChildBelongsToLead(lead, input.childId());

        List<GroupScheduleDto> groupSlots = input.groupId() == null
                ? List.of()
                : findActiveSlotsByGroup(input.groupId(), input.slot().date());
        List<GroupScheduleDto> coachSlots = input.coachId() == null
                ? List.of()
                : findActiveSlotsByCoach(input.coachId(), input.slot().date());

        GroupScheduleDto groupSlot = input.groupId() == null
                ? null
                : findMatchingSlot(groupSlots, input.slot().startTime(), "Group slot not found for provided date/start time");
        GroupScheduleDto coachSlot = input.coachId() == null
                ? null
                : findMatchingSlot(coachSlots, input.slot().startTime(), "Coach slot not found for provided date/start time");

        UUID resolvedGroupId = resolveGroupId(lead, input.groupId(), coachSlot);
        UUID resolvedCoachId = resolveCoachId(input.coachId(), groupSlot);

        validateGroupAndCoach(lead, resolvedGroupId, resolvedCoachId);
        ensureNoScheduleConflict(groupSlot, coachSlot, resolvedGroupId, resolvedCoachId);

        GroupScheduleDto selectedSlot = groupSlot != null ? groupSlot : coachSlot;

        LeadStatus previousStatus = lead.getStatus();

        lead.scheduleTrial(
                input.childId(),
                resolvedGroupId,
                resolvedCoachId,
                input.slot().date(),
                selectedSlot.startTime(),
                selectedSlot.endTime(),
                trim(input.comment())
        );
        LeadStatus newStatus = stateMachineService.process(leadId, previousStatus, LeadEvent.SCHEDULE_TRIAL);
        lead.updateStatus(newStatus);

        leadRepository.save(lead);
        leadActivityService.logStatusChanged(lead, LeadEvent.SCHEDULE_TRIAL, previousStatus, currentAdminId);
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
    public Map<LeadStatus, List<LeadOutput>> getKanban(UUID branchId, UUID currentAdminId) {
        var specification = LeadSpecification.build(
                null,
                null,
                branchId,
                null,
                null,
                null,
                null
        );

        List<Lead> filteredLeads = leadRepository.findAll(specification);

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
            outputColumns.put(
                    status,
                    leadColumns.get(status).stream()
                            .map(lead -> leadMapper.toOutput(lead, currentAdminId))
                            .toList()
            );
        }

        return outputColumns;
    }

    @Transactional(readOnly = true)
    public Lead getLeadById(UUID leadId) {
        return findById(leadId);
    }

    @Transactional(readOnly = true)
    @Override
    public List<LeadActivityOutput> getLeadActivities(UUID leadId) {
        findById(leadId);
        return leadActivityService.getLeadActivities(leadId);
    }

    @Transactional(readOnly = true)
    @Override
    public UUID getLeadBranchId(UUID leadId) {
        return findById(leadId).getBranchId();
    }

    @Transactional(readOnly = true)
    @Override
    public List<LeadLossReasonResponse> getActiveLossReasons() {
        return leadLossReasonRepository.findByActiveTrueOrderBySortOrderAscNameAsc()
                .stream()
                .map(reason -> new LeadLossReasonResponse(reason.getCode(), reason.getName()))
                .toList();
    }

    @Transactional
    @Override
    public void assignLead(UUID leadId, UUID assignedAdminId, UUID currentAdminId) {
        validateAssignedAdmin(assignedAdminId);

        Lead lead = findById(leadId);
        UUID previousAdminId = lead.getAssignedAdminId();
        lead.assignAdmin(assignedAdminId);

        leadRepository.save(lead);
        leadActivityService.logLeadAssigned(lead, previousAdminId, currentAdminId);
    }

    @Transactional
    @Override
    public LeadStatus processEvent(
            UUID leadId,
            LeadEvent event,
            String lostReasonCode,
            String lostComment,
            UUID currentAdminId
    ) {
        Lead lead = findById(leadId);
        LeadStatus previousStatus = lead.getStatus();

        LeadStatus newStatus = stateMachineService.process(lead.getId(), previousStatus, event);
        syncTrialStatusByEvent(lead, event);
        lead.updateStatus(newStatus);

        String activityDetailsOverride = null;
        if (newStatus == LeadStatus.LOST) {
            LeadLossReasonEntity reason = validateAndResolveLossReason(lostReasonCode, lostComment);
            lead.markLost(reason.getCode(), trim(lostComment), LocalDateTime.now());
            activityDetailsOverride = buildLostDetails(event, reason.getCode(), trim(lostComment));
        } else if (previousStatus == LeadStatus.LOST) {
            lead.clearLostSnapshot();
        }

        leadRepository.save(lead);
        leadActivityService.logStatusChanged(lead, event, previousStatus, currentAdminId, activityDetailsOverride);

        return newStatus;
    }

    private void syncTrialStatusByEvent(Lead lead, LeadEvent event) {
        if (event == LeadEvent.COMPLETE_TRIAL) {
            if (lead.getTrial() == null) {
                throw new BadRequestException("Trial is not scheduled", lead.getId());
            }
            lead.getTrial().markCompleted();
            return;
        }

        if (event == LeadEvent.NO_SHOW) {
            if (lead.getTrial() == null) {
                throw new BadRequestException("Trial is not scheduled", lead.getId());
            }
            lead.getTrial().markCanceled();
        }
    }

    private LeadLossReasonEntity validateAndResolveLossReason(String lostReasonCode, String lostComment) {
        if (lostReasonCode == null || lostReasonCode.isBlank()) {
            throw new BadRequestException("lostReasonCode is required for LOST transition");
        }

        LeadLossReasonEntity reason = leadLossReasonRepository.findByCodeAndActiveTrue(lostReasonCode.trim())
                .orElseThrow(() -> new BadRequestException("Invalid or inactive loss reason", lostReasonCode));

        if ("OTHER".equals(reason.getCode()) && (lostComment == null || lostComment.isBlank())) {
            throw new BadRequestException("lostComment is required for OTHER loss reason");
        }

        return reason;
    }

    private String buildLostDetails(LeadEvent event, String reasonCode, String lostComment) {
        try {
            Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("event", event.name());
            payload.put("lostReasonCode", reasonCode);
            payload.put("lostComment", lostComment);
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{\"event\":\"" + event.name() + "\",\"lostReasonCode\":\"" + reasonCode + "\"}";
        }
    }

    @Transactional
    @Override
    public UUID convertLeadToClient(UUID leadId, UUID currentAdminId) {
        Lead lead = findById(leadId);
        if (lead.getStatus() != LeadStatus.WON) {
            throw new BadRequestException("Only WON leads can be converted to client", leadId);
        }
        if (lead.getClientId() != null) {
            return lead.getClientId();
        }

        if (!lead.isReadyForConversion()) {
            throw new BadRequestException("Lead is not ready for conversion", leadId);
        }

        UUID clientId = clientPort.createClient(
                lead.getParentName(),
                lead.getPhone(),
                lead.getEmail()
        );

        List<LeadChildInput> children = lead.getChildren().stream()
                .map(child -> new LeadChildInput(
                        trim(child.getChildName()),
                        child.getChildAge(),
                        child.getGender(),
                        trim(child.getExperience())
                ))
                .toList();
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
        leadActivityService.logLeadConverted(lead, currentAdminId);

        log.info("Lead {} converted to client {}", leadId, clientId);

        return clientId;
    }

    private void replaceChildrenFromQualification(Lead lead, List<LeadChildInput> children) {
        if (children == null) {
            return;
        }

        lead.clearChildren();
        for (LeadChildInput child : children) {
            lead.addChild(
                    trim(child.childName()),
                    child.childAge(),
                    child.gender(),
                    trim(child.experience())
            );
        }
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
        if (input.slot() == null || input.slot().date() == null || input.slot().startTime() == null) {
            throw new BadRequestException("Slot date and start time are required");
        }
    }

    private void validateChildBelongsToLead(Lead lead, UUID childId) {
        boolean exists = lead.getChildren().stream()
                .anyMatch(child -> Objects.equals(child.getId(), childId));

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

    private List<GroupScheduleDto> findActiveSlotsByGroup(UUID groupId, java.time.LocalDate date) {
        return groupSchedulePort.getSchedules(
                ScheduleSearchCriteria.builder()
                        .groupId(groupId)
                        .fromDate(date)
                        .toDate(date)
                        .dayOfWeek(date.getDayOfWeek())
                        .status(ScheduleStatus.ACTIVE)
                        .build()
        );
    }

    private List<GroupScheduleDto> findActiveSlotsByCoach(UUID coachId, java.time.LocalDate date) {
        return groupSchedulePort.getSchedules(
                ScheduleSearchCriteria.builder()
                        .coachId(coachId)
                        .fromDate(date)
                        .toDate(date)
                        .dayOfWeek(date.getDayOfWeek())
                        .status(ScheduleStatus.ACTIVE)
                        .build()
        );
    }

    private GroupScheduleDto findMatchingSlot(List<GroupScheduleDto> slots, java.time.LocalTime startTime, String message) {
        return slots.stream()
                .filter(slot -> slot.startTime().equals(startTime))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(message));
    }

    private UUID resolveGroupId(Lead lead, UUID requestedGroupId, GroupScheduleDto coachSlot) {
        if (requestedGroupId != null) {
            return requestedGroupId;
        }
        if (coachSlot == null) {
            throw new BadRequestException("Unable to resolve group from slot");
        }
        var group = groupPort.getGroupById(coachSlot.groupId());
        if (!lead.getBranchId().equals(group.branchId())) {
            throw new BadRequestException("Group does not belong to lead branch", Map.of("branchId", lead.getBranchId(), "groupId", coachSlot.groupId()));
        }
        return coachSlot.groupId();
    }

    private UUID resolveCoachId(UUID requestedCoachId, GroupScheduleDto groupSlot) {
        if (requestedCoachId != null) {
            return requestedCoachId;
        }
        if (groupSlot == null) {
            throw new BadRequestException("Unable to resolve coach from slot");
        }
        return groupSlot.coachId();
    }

    private void ensureNoScheduleConflict(
            GroupScheduleDto groupSlot,
            GroupScheduleDto coachSlot,
            UUID resolvedGroupId,
            UUID resolvedCoachId
    ) {
        if (groupSlot == null || coachSlot == null) {
            return;
        }

        if (!groupSlot.startTime().equals(coachSlot.startTime()) || !groupSlot.endTime().equals(coachSlot.endTime())) {
            throw new BadRequestException("Selected group and coach slots conflict");
        }

        if (!groupSlot.coachId().equals(resolvedCoachId) || !coachSlot.groupId().equals(resolvedGroupId)) {
            throw new BadRequestException("Coach and group are not available in the same schedule slot");
        }
    }

}
