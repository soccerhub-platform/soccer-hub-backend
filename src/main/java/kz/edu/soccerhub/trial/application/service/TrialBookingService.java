package kz.edu.soccerhub.trial.application.service;

import jakarta.transaction.Transactional;
import kz.edu.soccerhub.common.dto.trial.*;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.TrialLeadPort;
import kz.edu.soccerhub.common.port.TrialGroupPort;
import kz.edu.soccerhub.common.port.TrialPort;
import kz.edu.soccerhub.common.port.TrialSessionPort;
import kz.edu.soccerhub.common.port.TrialStudentPort;
import kz.edu.soccerhub.trial.domain.entity.TrialBooking;
import kz.edu.soccerhub.trial.domain.enums.TrialBookingStatus;
import kz.edu.soccerhub.trial.domain.repository.TrialBookingRepository;
import kz.edu.soccerhub.trial.domain.repository.TrialBookingSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class TrialBookingService implements TrialPort {

    private final TrialBookingRepository repository;
    private final TrialGroupPort groupPort;
    private final TrialSessionPort sessionPort;
    private final TrialStudentPort studentPort;
    private final TrialLeadPort leadPort;
    private final TrialBookingDetailsReader detailsReader;

    @Override
    @Transactional
    public TrialBookingDto createTrial(
            CreateTrialBookingCommand command
    ) {
        validateCommand(command);

        TrialSessionContext session = sessionPort.getBookableSession(command.trainingSessionId());

        if (command.leadId() != null) {
            if (command.participantId() != null) {
                leadPort.validateParticipant(command.leadId(), command.participantId());
            } else if (command.studentId() != null) {
                leadPort.validateConvertedStudent(command.leadId(), command.studentId());
            } else {
                throw new BadRequestException("Lead participant is required");
            }
        } else if (command.studentId() == null) {
            throw new BadRequestException("Student is required when trial is not linked to a lead");
        }

        if (command.studentId() != null) {
            studentPort.validateExists(command.studentId());
            groupPort.validateAvailableCapacity(
                    session.groupId(),
                    command.studentId(),
                    session.startsAt().toLocalDate()
            );
        }

        boolean alreadyBooked = command.studentId() != null &&
                repository.existsByStudentIdAndTrainingSessionIdAndStatusIn(
                        command.studentId(),
                        session.sessionId(),
                        List.of(
                                TrialBookingStatus.SCHEDULED,
                                TrialBookingStatus.CONFIRMED
                        )
                );

        if (alreadyBooked) {
            throw new BadRequestException(
                    "Student already has an active trial for this session"
            );
        }

        TrialBooking booking = TrialBooking.schedule(
                command.leadId(),
                command.clientId(),
                command.participantId(),
                command.studentId(),
                session.sessionId()
        );

        return TrialBookingDto.from(repository.save(booking));
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<TrialBookingDto> findTrials(
            TrialBookingSearchCommand command,
            Pageable pageable
    ) {
        if (command == null) {
            throw new BadRequestException("Trial search query is required");
        }

        return repository
                .findAll(TrialBookingSpecifications.byQuery(command), pageable)
                .map(TrialBookingDto::from);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public TrialBookingDto getTrial(UUID trialId) {
        TrialBooking booking = repository.findById(trialId)
                .orElseThrow(() -> new kz.edu.soccerhub.common.exception.NotFoundException(
                        "Trial booking not found",
                        trialId
                ));

        return TrialBookingDto.from(booking);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public TrialBookingDetailsDto getTrialDetails(UUID trialId) {
        TrialBooking booking = repository.findById(trialId)
                .orElseThrow(() -> new NotFoundException(
                        "Trial booking not found",
                        trialId
                ));

        return detailsReader.read(booking);
    }

    @Override
    @Transactional
    public TrialBookingDetailsDto confirmTrial(UUID trialId, UUID adminId) {
        requireAdminId(adminId);

        TrialBooking booking = getBooking(trialId);
        booking.confirm();
        return detailsReader.read(booking);
    }

    @Override
    @Transactional
    public TrialBookingDetailsDto cancelTrial(CancelTrialCommand command) {
        validateCancelCommand(command);

        TrialBooking booking = getBooking(command.trialId());
        booking.cancel(command.reason());
        return detailsReader.read(booking);
    }

    @Override
    @Transactional
    public TrialBookingDetailsDto markAttendance(
            MarkTrialAttendanceCommand command
    ) {
        if (command == null || command.trialId() == null) {
            throw new BadRequestException("Trial id is required");
        }

        if (command.status() == null) {
            throw new BadRequestException("Attendance status is required");
        }

        requireAdminId(command.adminId());

        TrialBooking booking = getBooking(command.trialId());
        booking.markAttendance(
                command.status(),
                command.adminId(),
                command.comment()
        );

        return detailsReader.read(booking);
    }

    @Override
    @Transactional
    public TrialBookingDetailsDto recordResult(
            RecordTrialResultCommand command
    ) {
        if (command == null || command.trialId() == null) {
            throw new BadRequestException("Trial id is required");
        }

        if (command.result() == null) {
            throw new BadRequestException("Trial result is required");
        }

        requireAdminId(command.adminId());

        TrialBooking booking = getBooking(command.trialId());
        booking.recordResult(
                command.result(),
                command.recommendedGroupId(),
                command.coachFeedback()
        );

        return detailsReader.read(booking);
    }

    @Override
    @Transactional
    public void linkConvertedStudent(LinkTrialStudentCommand command) {
        if (command == null || command.leadId() == null || command.participantId() == null
                || command.studentId() == null) {
            throw new BadRequestException("Lead, participant and student are required");
        }

        leadPort.validateParticipant(command.leadId(), command.participantId());
        repository.findAllByLeadIdAndParticipantIdAndStudentIdIsNull(
                        command.leadId(), command.participantId())
                .forEach(booking -> booking.linkStudent(command.clientId(), command.studentId()));
    }

    private TrialBooking getBooking(UUID trialId) {
        if (trialId == null) {
            throw new BadRequestException("Trial id is required");
        }

        return repository.findById(trialId)
                .orElseThrow(() -> new NotFoundException(
                        "Trial booking not found",
                        trialId
                ));
    }

    private void requireAdminId(UUID adminId) {
        if (adminId == null) {
            throw new BadRequestException("Admin id is required");
        }
    }

    private void validateCancelCommand(CancelTrialCommand command) {
        if (command == null || command.trialId() == null) {
            throw new BadRequestException("Trial id is required");
        }

        requireAdminId(command.adminId());

        if (command.reason() == null || command.reason().isBlank()) {
            throw new BadRequestException("Cancellation reason is required");
        }
    }

    private void validateCommand(CreateTrialBookingCommand command) {
        if (command == null) {
            throw new BadRequestException(
                    "Trial booking command is required"
            );
        }

        if (command.studentId() == null && (command.leadId() == null || command.participantId() == null)) {
            throw new BadRequestException(
                    "Student or lead participant is required"
            );
        }

        if (command.trainingSessionId() == null) {
            throw new BadRequestException(
                    "Training session id is required"
            );
        }

        if (command.adminId() == null) {
            throw new BadRequestException(
                    "Admin id is required"
            );
        }
    }
}
