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

    @Transactional
    public TrialBookingDto create(
            UUID adminId,
            CreateTrialBookingInput input
    ) {
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
            String reason
    ) {
        AdminTrialDetailsOutput output = AdminTrialDetailsOutput.from(
                trialPort.cancelTrial(
                        CancelTrialCommand.builder()
                                .trialId(trialId)
                                .adminId(adminId)
                                .reason(reason)
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

    @Transactional
    public AdminTrialDetailsOutput reschedule(
            UUID trialId,
            UUID adminId,
            AdminRescheduleTrialInput input
    ) {
        return AdminTrialDetailsOutput.from(
                trialPort.rescheduleTrial(
                        RescheduleTrialBookingCommand.builder()
                                .trialId(trialId)
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

        AdminTrialDetailsOutput output = AdminTrialDetailsOutput.from(
                trialPort.markAttendance(
                        MarkTrialAttendanceCommand.builder()
                                .trialId(trialId)
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
        return AdminTrialDetailsOutput.from(
                trialPort.recordResult(
                        RecordTrialResultCommand.builder()
                                .trialId(trialId)
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
