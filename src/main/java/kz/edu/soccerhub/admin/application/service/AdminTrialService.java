package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.trial.*;
import kz.edu.soccerhub.common.dto.trial.*;
import kz.edu.soccerhub.common.port.TrialPort;
import kz.edu.soccerhub.common.port.LeadPort;
import kz.edu.soccerhub.crm.application.state.LeadEvent;
import kz.edu.soccerhub.trial.domain.enums.TrialAttendanceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminTrialService {

    private final TrialPort trialPort;
    private final LeadPort leadPort;
    private final kz.edu.soccerhub.common.port.AdminBranchAccessPort branchAccess;
    private final kz.edu.soccerhub.common.port.TrialSessionPort sessions;
    private final kz.edu.soccerhub.common.port.GroupPort groups;
    private final kz.edu.soccerhub.common.port.CompanyOrganizationPort organization;
    private final kz.edu.soccerhub.common.port.CompanySessionReadPort sessionReader;
    private final kz.edu.soccerhub.common.port.TrialWorkflowPort workflow;

    public void requireSessionAccess(UUID actor, UUID sessionId) {
        UUID branch=groups.getGroupById(sessions.getSessionDetails(sessionId).groupId()).branchId();
        requireBranch(actor,branch);
    }
    public void requireBranch(UUID actor,UUID branch) {
        if(branch==null || !branchAccess.verifyAdminBelongsToBranch(actor,branch)) throw new kz.edu.soccerhub.common.exception.ForbiddenException("Нет доступа к филиалу");
    }
    public void requireTrialAccess(UUID actor,UUID trialId) {requireSessionAccess(actor,trialPort.getTrial(trialId).trainingSessionId());}
    public java.util.List<kz.edu.soccerhub.common.dto.company.TimelineEvent> history(UUID actor,UUID trialId) {requireTrialAccess(actor,trialId);return workflow.history(trialId);}
    public Page<TrialBookingListItemDto> scopedList(UUID actor,UUID branch,TrialBookingSearchCommand c,Pageable pageable) {
        requireBranch(actor,branch);
        if(!java.util.Set.of("createdAt","id").containsAll(pageable.getSort().stream().map(org.springframework.data.domain.Sort.Order::getProperty).toList())) throw new kz.edu.soccerhub.common.exception.BadRequestException("Недопустимая сортировка пробных");
        var ids=sessionReader.sessions(organization.groupBranches(java.util.Set.of(branch)).keySet(),null,null).keySet();
        return trialPort.findList(new TrialBookingSearchCommand(c.status(),c.leadId(),c.clientId(),c.studentId(),c.trainingSessionId(),ids),pageable);
    }

    @Transactional
    public TrialBookingDto create(
            UUID adminId,
            CreateTrialBookingInput input
    ) {
        requireSessionAccess(adminId,input.trainingSessionId());
        TrialBookingDto booking = trialPort.createTrial(
                CreateTrialBookingCommand.builder()
                        .leadId(input.leadId())
                        .clientId(input.clientId())
                        .participantId(input.participantId())
                        .studentId(input.studentId())
                        .trainingSessionId(input.trainingSessionId())
                        .adminId(adminId)
                        .build()
        );

        if (booking.leadId() != null) {
            leadPort.processEvent(
                    booking.leadId(),
                    LeadEvent.SCHEDULE_TRIAL,
                    null,
                    null,
                    adminId
            );
        }

        return booking;
    }

    @Transactional(readOnly = true)
    public Page<TrialBookingListItemDto> findList(
            TrialBookingSearchCommand command,
            Pageable pageable
    ) {
        return trialPort.findList(command, pageable);
    }

    @Transactional
    public TrialBookingDto get(UUID trialId) {
        return trialPort.getTrial(trialId);
    }

    @Transactional(readOnly = true)
    public AdminTrialDetailsOutput getDetails(UUID trialId) {
        return AdminTrialDetailsOutput.from(
                trialPort.getTrialDetails(trialId)
        );
    }

    @Transactional
    public AdminTrialDetailsOutput cancel(
            UUID trialId,
            UUID adminId,
            String reason, Long version
    ) {
        requireTrialAccess(adminId,trialId);
        AdminTrialDetailsOutput output = AdminTrialDetailsOutput.from(
                trialPort.cancelTrial(
                        CancelTrialCommand.builder()
                                .trialId(trialId)
                                .adminId(adminId)
                                .reason(reason).version(version)
                                .build()
                )
        );

        if (output.lead() != null) {
            leadPort.processEvent(
                    output.lead().id(),
                    LeadEvent.CANCEL_TRIAL,
                    null,
                    null,
                    adminId
            );
        }

        return output;
    }

    public AdminTrialDetailsOutput cancel(UUID trialId, UUID adminId, String reason) { return cancel(trialId, adminId, reason, null); }

    @Transactional
    public AdminTrialDetailsOutput reschedule(
            UUID trialId,
            UUID adminId,
            AdminRescheduleTrialInput input
    ) {
        requireTrialAccess(adminId,trialId); requireSessionAccess(adminId,input.trainingSessionId());
        return AdminTrialDetailsOutput.from(
                trialPort.rescheduleTrial(
                        RescheduleTrialBookingCommand.builder()
                                .trialId(trialId).version(input.version())
                                .trainingSessionId(input.trainingSessionId())
                                .adminId(adminId)
                                .build()
                )
        );
    }

    @Transactional
    public AdminTrialDetailsOutput markAttendance(
            UUID trialId,
            UUID adminId,
            AdminMarkTrialAttendanceInput input
    ) {

        requireTrialAccess(adminId,trialId);
        AdminTrialDetailsOutput output = AdminTrialDetailsOutput.from(
                trialPort.markAttendance(
                        MarkTrialAttendanceCommand.builder()
                                .trialId(trialId).version(input.version())
                                .adminId(adminId)
                                .status(input.status())
                                .comment(input.comment())
                                .build()
                )
        );

        syncLeadAfterTrialAttendance(output, input.status(), adminId);

        return output;
    }

    private void syncLeadAfterTrialAttendance(
            AdminTrialDetailsOutput output,
            TrialAttendanceStatus attendanceStatus,
            UUID actorId
    ) {
        if (output.lead() == null) {
            return;
        }

        LeadEvent event = attendanceStatus == TrialAttendanceStatus.ATTENDED
                ? LeadEvent.COMPLETE_TRIAL
                : LeadEvent.NO_SHOW;

        try {
            leadPort.processEvent(
                    output.lead().id(),
                    event,
                    null,
                    null,
                    actorId
            );
        } catch (kz.edu.soccerhub.common.exception.BadRequestException exception) {
            log.warn(
                    "Lead sync skipped after trial attendance: leadId={}, trialId={}, event={}, reason={}",
                    output.lead().id(),
                    output.id(),
                    event,
                    exception.getMessage()
            );
        }
    }

    @Transactional
    public AdminTrialDetailsOutput recordResult(
            UUID trialId,
            UUID adminId,
            AdminRecordTrialResultInput input
    ) {
        requireTrialAccess(adminId,trialId);
        return AdminTrialDetailsOutput.from(
                trialPort.recordResult(
                        RecordTrialResultCommand.builder()
                                .trialId(trialId).version(input.version())
                                .adminId(adminId)
                                .result(input.result())
                                .recommendedGroupId(input.recommendedGroupId())
                                .coachFeedback(input.coachFeedback())
                                .nextActionType(input.nextActionType())
                                .nextActionAt(input.nextActionAt())
                                .build()
                )
        );
    }
}
