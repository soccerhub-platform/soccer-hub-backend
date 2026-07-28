package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.trial.AdminTrialDetailsOutput;
import kz.edu.soccerhub.admin.application.dto.trial.AdminMarkTrialAttendanceInput;
import kz.edu.soccerhub.admin.application.dto.trial.AdminRecordTrialResultInput;
import kz.edu.soccerhub.admin.application.dto.trial.CreateTrialBookingInput;
import kz.edu.soccerhub.common.dto.trial.CancelTrialCommand;
import kz.edu.soccerhub.common.dto.trial.CreateTrialBookingCommand;
import kz.edu.soccerhub.common.dto.trial.MarkTrialAttendanceCommand;
import kz.edu.soccerhub.common.dto.trial.RecordTrialResultCommand;
import kz.edu.soccerhub.common.dto.trial.TrialBookingDto;
import kz.edu.soccerhub.common.dto.trial.TrialBookingSearchCommand;
import kz.edu.soccerhub.common.port.TrialPort;
import kz.edu.soccerhub.common.port.LeadPort;
import kz.edu.soccerhub.crm.application.state.LeadEvent;
import kz.edu.soccerhub.trial.domain.enums.TrialAttendanceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
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

        if (input.leadId() != null) {
            leadPort.processEvent(
                    input.leadId(),
                    LeadEvent.SCHEDULE_TRIAL,
                    null,
                    null,
                    adminId
            );
        }

        return booking;
    }

    @Transactional
    public Page<TrialBookingDto> find(
            TrialBookingSearchCommand command,
            Pageable pageable
    ) {
        return trialPort.findTrials(command, pageable);
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
    public AdminTrialDetailsOutput confirm(UUID trialId, UUID adminId) {
        return AdminTrialDetailsOutput.from(
                trialPort.confirmTrial(trialId, adminId)
        );
    }

    @Transactional
    public AdminTrialDetailsOutput cancel(
            UUID trialId,
            UUID adminId,
            String reason
    ) {
        return AdminTrialDetailsOutput.from(
                trialPort.cancelTrial(
                        CancelTrialCommand.builder()
                                .trialId(trialId)
                                .adminId(adminId)
                                .reason(reason)
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

        if (output.lead() != null) {
            LeadEvent event = input.status() == TrialAttendanceStatus.ATTENDED
                    ? LeadEvent.COMPLETE_TRIAL
                    : LeadEvent.NO_SHOW;

            leadPort.processEvent(
                    output.lead().id(),
                    event,
                    null,
                    null,
                    adminId
            );
        }

        return output;
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
