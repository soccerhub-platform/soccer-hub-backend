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

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<TrialBookingListItemDto> findBySessionIds(java.util.Collection<UUID> sessionIds) {
        if (sessionIds.isEmpty()) return List.of();
        return listReader.read(new org.springframework.data.domain.PageImpl<>(
                repository.findAllByTrainingSessionIdInOrderByCreatedAtAsc(sessionIds))).getContent();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<TrialBookingListItemDto> findByLeadIds(java.util.Collection<UUID> leadIds) {
        if (leadIds.isEmpty()) return List.of();
        return listReader.read(new org.springframework.data.domain.PageImpl<>(
                repository.findAllByLeadIdInOrderByCreatedAtDesc(leadIds))).getContent();
    }

    private static final List<TrialBookingStatus> ACTIVE_STATUSES = List.of(
            TrialBookingStatus.SCHEDULED
    );

    private final TrialBookingRepository repository;
    private final TrialOperationJournal journal;
    private final TrialGroupPort groupPort;
    private final TrialSessionPort sessionPort;
    private final TrialStudentPort studentPort;
    private final TrialLeadPort leadPort;
    private final TrialBookingDetailsReader detailsReader;
    private final TrialBookingListReader listReader;
    private final TrialSessionParticipantReader sessionParticipantReader;

    @Override
    @Transactional
    public TrialBookingDto createTrial(
            CreateTrialBookingCommand command
    ) {
        validateCommand(command);

        TrialSessionContext session = sessionPort.getBookableSession(command.trainingSessionId());

        if (command.leadId() != null) {
            leadPort.validateBranch(command.leadId(),groupPort.branchId(session.groupId()));
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
            studentPort.validateBranch(command.studentId(),groupPort.branchId(session.groupId()));
        }

        groupPort.validateAvailableCapacity(session.groupId(),command.studentId(),session.startsAt().toLocalDate());
        ensureNoActiveTrial(command);

        TrialBooking booking = TrialBooking.schedule(
                command.leadId(),
                command.clientId(),
                command.participantId(),
                command.studentId(),
                session.sessionId()
        );

        var saved=repository.save(booking);
        repository.flush();
        journal.append(saved.getId(),command.adminId(),"CREATE","Запись на пробное · " + session.startsAt());
        return TrialBookingDto.from(saved);
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
    public TrialBookingDetailsDto cancelTrial(CancelTrialCommand command) {
        validateCancelCommand(command);

        TrialBooking booking = lockedBooking(command.trialId(), command.version());
        booking.cancel(command.reason());
        saveOperation(booking, command.adminId(), "CANCEL", "Отмена · " + command.reason());
        return detailsReader.read(booking);
    }

    @Override
    @Transactional
    public TrialBookingDetailsDto rescheduleTrial(
            RescheduleTrialBookingCommand command
    ) {
        validateRescheduleCommand(command);

        TrialBooking booking = lockedBooking(command.trialId(), command.version());

        TrialSessionContext session = sessionPort.getBookableSession(
                command.trainingSessionId()
        );

        if(booking.getLeadId()!=null)leadPort.validateBranch(booking.getLeadId(),groupPort.branchId(session.groupId()));
        if(booking.getStudentId()!=null)studentPort.validateBranch(booking.getStudentId(),groupPort.branchId(session.groupId()));
        groupPort.validateAvailableCapacity(session.groupId(),booking.getStudentId(),session.startsAt().toLocalDate());

        booking.reschedule(session.sessionId());
        saveOperation(booking, command.adminId(), "RESCHEDULE", "Перенос · " + session.startsAt());

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

        TrialBooking booking = lockedBooking(command.trialId(), command.version());
        if (command.status() != kz.edu.soccerhub.trial.domain.enums.TrialAttendanceStatus.ATTENDED
                && command.status() != kz.edu.soccerhub.trial.domain.enums.TrialAttendanceStatus.NO_SHOW)
            throw new BadRequestException("Выберите посещение или неявку");
        if (booking.getResult() == kz.edu.soccerhub.trial.domain.enums.TrialResult.CONVERTED)
            throw new BadRequestException("Пробная уже связана с оформлением");
        var session = sessionPort.getSessionDetails(booking.getTrainingSessionId());
        if (session.startsAt().isAfter(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Almaty"))))
            throw new BadRequestException("Посещение можно отметить после начала занятия");
        booking.markAttendance(
                command.status(),
                command.adminId(),
                command.comment()
        );

        saveOperation(booking, command.adminId(), "ATTENDANCE", "Посещение · " + (command.status() == kz.edu.soccerhub.trial.domain.enums.TrialAttendanceStatus.ATTENDED ? "Посетил" : "Не пришёл") + comment(command.comment()));
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

        TrialBooking booking = lockedBooking(command.trialId(), command.version());
        if (command.result() == kz.edu.soccerhub.trial.domain.enums.TrialResult.CONVERTED && (booking.getStudentId()==null||booking.getClientId()==null))
            throw new BadRequestException("Сначала оформите ученика в карточке лида");
        if (booking.getResult() == kz.edu.soccerhub.trial.domain.enums.TrialResult.CONVERTED
                || command.result() == kz.edu.soccerhub.trial.domain.enums.TrialResult.PENDING)
            throw new BadRequestException("Результат недоступен для изменения");
        if (command.result() == kz.edu.soccerhub.trial.domain.enums.TrialResult.FOLLOW_UP
                && (command.nextActionAt() == null || !command.nextActionAt().isAfter(java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Almaty")))))
            throw new BadRequestException("Укажите будущий срок следующего действия");
        validateRecommendedGroup(booking, command.recommendedGroupId());
        booking.recordResult(
                command.result(),
                command.recommendedGroupId(),
                command.coachFeedback(),
                command.nextActionType(),
                command.nextActionAt()
        );

        saveOperation(booking, command.adminId(), "RESULT", "Результат · " + command.result() + (command.nextActionAt() == null ? "" : " · " + command.nextActionType() + " · " + command.nextActionAt()) + comment(command.coachFeedback()));
        return detailsReader.read(booking);
    }

    @Override
    @Transactional
    public void linkConvertedStudent(LinkTrialStudentCommand command) {
        if (command == null || command.leadId() == null || command.participantId() == null
                || command.studentId() == null || command.actorId()==null) {
            throw new BadRequestException("Lead, participant and student are required");
        }

        leadPort.validateParticipant(command.leadId(), command.participantId());
        repository.findAllByLeadIdAndParticipantIdAndStudentIdIsNull(
                        command.leadId(), command.participantId())
                .forEach(booking -> {
                    var locked=repository.findLockedById(booking.getId()).orElseThrow(()->new NotFoundException("Trial not found",booking.getId()));
                    locked.linkStudent(command.clientId(),command.studentId());
                    if(locked.getAttendanceStatus()==kz.edu.soccerhub.trial.domain.enums.TrialAttendanceStatus.ATTENDED) {
                        locked.recordResult(kz.edu.soccerhub.trial.domain.enums.TrialResult.CONVERTED,locked.getRecommendedGroupId(),locked.getCoachFeedback(),null,null);
                    }
                    saveOperation(locked,command.actorId(),"CONVERSION","Ученик оформлен · " + command.studentId());
                });
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public Page<TrialBookingListItemDto> findList(
            TrialBookingSearchCommand command,
            Pageable pageable
    ) {
        if (command == null) {
            throw new BadRequestException("Trial search query is required");
        }

        Page<TrialBooking> bookings = repository.findAll(
                TrialBookingSpecifications.byQuery(command),
                pageable
        );

        return listReader.read(bookings);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public List<TrialSessionParticipantDto> getSessionParticipants(
            UUID trainingSessionId
    ) {
        if (trainingSessionId == null) {
            throw new BadRequestException(
                    "Training session id is required"
            );
        }

        List<TrialBooking> bookings =
                repository
                        .findAllByTrainingSessionIdAndStatusInOrderByCreatedAtAsc(
                                trainingSessionId,
                                List.of(
                                        TrialBookingStatus.SCHEDULED,
                                        TrialBookingStatus.COMPLETED
                                )
                        );

        return sessionParticipantReader.read(bookings);
    }

    @Override
    @Transactional
    public TrialSessionParticipantDto recordCoachRecommendation(
            RecordTrialCoachRecommendationCommand command
    ) {
        validateCoachRecommendationCommand(command);

        TrialBooking booking = lockedBooking(command.trialId(), command.version());

        validateRecommendedGroup(booking, command.recommendedGroupId());

        booking.recordCoachRecommendation(
                command.recommendation(),
                command.recommendedGroupId(),
                command.comment(),
                command.coachId()
        );

        saveOperation(booking, command.coachId(), "RECOMMENDATION", "Рекомендация тренера · " + command.recommendation() + comment(command.comment()));
        return sessionParticipantReader
                .read(List.of(booking))
                .getFirst();
    }

    private String comment(String value) { return value == null || value.isBlank() ? "" : "\n" + value.trim(); }

    private void saveOperation(TrialBooking booking, UUID actor, String type, String description) {
        repository.saveAndFlush(booking);
        journal.append(booking.getId(), actor, type, description);
    }

    private void validateRecommendedGroup(TrialBooking booking, UUID groupId) {
        if (groupId == null) return;
        groupPort.validateSameBranch(sessionPort.getSessionDetails(booking.getTrainingSessionId()).groupId(),groupId);
        groupPort.getDetails(groupId);
        groupPort.validateAvailableCapacity(groupId, booking.getStudentId(), java.time.LocalDate.now(java.time.ZoneId.of("Asia/Almaty")));
    }

    private TrialBooking lockedBooking(UUID trialId, Long version) {
        if (version == null || version < 0) throw new BadRequestException("Укажите версию пробного");
        TrialBooking booking = repository.findLockedById(trialId).orElseThrow(() -> new NotFoundException("Trial booking not found", trialId));
        if (booking.getVersion() != version) throw new kz.edu.soccerhub.common.exception.ConflictException("Пробная уже изменена. Обновите карточку.", "TRIAL_WORK_CONFLICT", java.util.Map.of("trialId", trialId));
        return booking;
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

    private void validateRescheduleCommand(
            RescheduleTrialBookingCommand command
    ) {
        if (command == null || command.trialId() == null) {
            throw new BadRequestException("Trial id is required");
        }

        if (command.trainingSessionId() == null) {
            throw new BadRequestException(
                    "New training session id is required"
            );
        }

        requireAdminId(command.adminId());
    }

    private void validateCoachRecommendationCommand(RecordTrialCoachRecommendationCommand command) {
        if (command == null || command.trialId() == null) {
            throw new BadRequestException(
                    "Trial id is required"
            );
        }

        if (command.coachId() == null) {
            throw new BadRequestException(
                    "Coach id is required"
            );
        }

        if (command.recommendation() == null) {
            throw new BadRequestException(
                    "Coach recommendation is required"
            );
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

    private void ensureNoActiveTrial(CreateTrialBookingCommand command) {
        boolean participantAlreadyBooked =
                command.leadId() != null
                        && command.participantId() != null
                        && repository.existsByLeadIdAndParticipantIdAndStatusIn(
                                command.leadId(),
                                command.participantId(),
                                ACTIVE_STATUSES
                        );

        boolean studentAlreadyBooked =
                command.studentId() != null
                        && repository.existsByStudentIdAndStatusIn(
                                command.studentId(),
                                ACTIVE_STATUSES
                        );

        if (participantAlreadyBooked || studentAlreadyBooked) {
            throw new BadRequestException(
                    "Child already has an active trial booking"
            );
        }
    }
}
