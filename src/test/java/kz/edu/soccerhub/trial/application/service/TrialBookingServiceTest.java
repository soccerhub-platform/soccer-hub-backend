package kz.edu.soccerhub.trial.application.service;

import kz.edu.soccerhub.common.dto.trial.*;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.port.TrialGroupPort;
import kz.edu.soccerhub.common.port.TrialLeadPort;
import kz.edu.soccerhub.common.port.TrialSessionPort;
import kz.edu.soccerhub.common.port.TrialStudentPort;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialBookingStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialAttendanceStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialCoachRecommendation;
import kz.edu.soccerhub.trial.domain.enums.TrialResult;
import kz.edu.soccerhub.trial.domain.entity.TrialBooking;
import kz.edu.soccerhub.trial.domain.repository.TrialBookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrialBookingServiceTest {

    @Mock
    private TrialBookingRepository repository;
    @Mock
    private TrialGroupPort groupPort;
    @Mock
    private TrialSessionPort sessionPort;
    @Mock
    private TrialStudentPort studentPort;
    @Mock
    private TrialLeadPort leadPort;
    @Mock
    private TrialBookingDetailsReader detailsReader;
    @Mock
    private TrialBookingListReader listReader;
    @Mock
    private TrialSessionParticipantReader sessionParticipantReader;

    @Mock private TrialOperationJournal journal;
    private TrialBookingService service;

    @BeforeEach
    void setUp() {
        service = new TrialBookingService(
                repository, journal,
                groupPort,
                sessionPort,
                studentPort,
                leadPort,
                detailsReader,
                listReader,
                sessionParticipantReader
        );
    }

    @Test
    void createsTrialForBookableSession() {
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();

        when(sessionPort.getBookableSession(sessionId))
                .thenReturn(sessionContext(sessionId, groupId));

        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.createTrial(command(leadId, studentId, sessionId));

        assertEquals(studentId, result.studentId());
        assertEquals(sessionId, result.trainingSessionId());
        assertEquals(TrialBookingStatus.SCHEDULED, result.status());
        verify(leadPort).validateParticipant(leadId, studentId);
        verify(groupPort).validateAvailableCapacity(eq(groupId), eq(studentId), any());
    }

    @Test
    void dashboardTrialsUseOnlySuppliedSessionsAndKeepAllBookingStates() {
        UUID sessionId = UUID.randomUUID();
        assertEquals(List.of(), service.findBySessionIds(List.of()));
        org.mockito.Mockito.verifyNoInteractions(repository, listReader);
        var booking = TrialBooking.builder().id(UUID.randomUUID()).trainingSessionId(sessionId).build();
        when(repository.findAllByTrainingSessionIdInOrderByCreatedAtAsc(List.of(sessionId))).thenReturn(List.of(booking));
        var row = TrialBookingListItemDto.builder().id(booking.getId()).status(TrialBookingStatus.CANCELED).build();
        when(listReader.read(any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(row)));
        assertEquals(List.of(row), service.findBySessionIds(List.of(sessionId)));
        verify(repository).findAllByTrainingSessionIdInOrderByCreatedAtAsc(List.of(sessionId));
    }

    @Test
    void rejectsDuplicateActiveTrial() {
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(sessionPort.getBookableSession(sessionId))
                .thenReturn(sessionContext(sessionId, UUID.randomUUID()));
        when(repository.existsByStudentIdAndStatusIn(
                eq(studentId),
                eq(List.of(TrialBookingStatus.SCHEDULED))
        )).thenReturn(true);

        assertThrows(
                BadRequestException.class,
                () -> service.createTrial(command(null, studentId, sessionId))
        );

        verify(repository, never()).save(any());
    }

    @Test
    void rejectsSecondActiveTrialForLeadParticipant() {
        UUID leadId = UUID.randomUUID();
        UUID participantId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        when(sessionPort.getBookableSession(sessionId))
                .thenReturn(sessionContext(sessionId, UUID.randomUUID()));

        when(repository.existsByLeadIdAndParticipantIdAndStatusIn(
                leadId,
                participantId,
                List.of(TrialBookingStatus.SCHEDULED)
        )).thenReturn(true);

        CreateTrialBookingCommand command = CreateTrialBookingCommand.builder()
                .leadId(leadId)
                .participantId(participantId)
                .trainingSessionId(sessionId)
                .adminId(UUID.randomUUID())
                .build();

        assertThrows(
                BadRequestException.class,
                () -> service.createTrial(command)
        );

        verify(repository, never()).save(any());
    }

    @Test
    void propagatesGroupCapacityValidation() {
        UUID studentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        when(sessionPort.getBookableSession(sessionId))
                .thenReturn(sessionContext(sessionId, groupId));
        doThrow(new BadRequestException("Group has no available places"))
                .when(groupPort)
                .validateAvailableCapacity(eq(groupId), eq(studentId), any());

        assertThrows(
                BadRequestException.class,
                () -> service.createTrial(command(null, studentId, sessionId))
        );

        verify(repository, never()).save(any());
    }

    @Test
    void cancelsTrialWithReason() {
        TrialBooking booking = createBooking();
        UUID adminId = UUID.randomUUID();

        when(repository.findLockedById(booking.getId()))
                .thenReturn(Optional.of(booking));

        service.cancelTrial(
                CancelTrialCommand.builder().version(0L)
                        .trialId(booking.getId())
                        .adminId(adminId)
                        .reason("Student unavailable")
                        .build()
        );

        assertEquals(TrialBookingStatus.CANCELED, booking.getStatus());
        assertEquals("Student unavailable", booking.getCancellationReason());
    }

    @Test
    void marksAttendanceAndCompletesTrial() {
        TrialBooking booking = createBooking();
        UUID adminId = UUID.randomUUID();

        when(repository.findLockedById(booking.getId()))
                .thenReturn(Optional.of(booking));

        when(sessionPort.getSessionDetails(booking.getTrainingSessionId())).thenReturn(TrialSessionContext.builder().startsAt(LocalDateTime.now().minusDays(1)).build());
        service.markAttendance(
                MarkTrialAttendanceCommand.builder().version(0L)
                        .trialId(booking.getId())
                        .adminId(adminId)
                        .status(TrialAttendanceStatus.ATTENDED)
                        .comment("Attended trial")
                        .build()
        );

        assertEquals(TrialBookingStatus.COMPLETED, booking.getStatus());
        assertEquals(TrialAttendanceStatus.ATTENDED, booking.getAttendanceStatus());
    }

    @Test
    void recordsResultOnlyAfterAttendance() {
        TrialBooking booking = createBooking();
        UUID adminId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        booking.markAttendance(
                TrialAttendanceStatus.ATTENDED,
                adminId,
                null
        );

        when(repository.findLockedById(booking.getId()))
                .thenReturn(Optional.of(booking));

        when(sessionPort.getSessionDetails(booking.getTrainingSessionId())).thenReturn(TrialSessionContext.builder().groupId(groupId).build());
        service.recordResult(
                RecordTrialResultCommand.builder().version(0L)
                        .trialId(booking.getId())
                        .adminId(adminId)
                        .result(TrialResult.INTERESTED)
                        .recommendedGroupId(groupId)
                        .coachFeedback("Ready for group")
                        .build()
        );

        assertEquals(TrialResult.INTERESTED, booking.getResult());
        assertEquals(groupId, booking.getRecommendedGroupId());
    }

    @Test
    void reschedulesTrialToBookableSession() {
        TrialBooking booking = createBooking();
        UUID newSessionId = UUID.randomUUID();
        UUID newGroupId = UUID.randomUUID();

        when(repository.findLockedById(booking.getId()))
                .thenReturn(Optional.of(booking));

        when(sessionPort.getBookableSession(newSessionId))
                .thenReturn(sessionContext(newSessionId, newGroupId));

        service.rescheduleTrial(
                RescheduleTrialBookingCommand.builder().version(0L)
                        .trialId(booking.getId())
                        .trainingSessionId(newSessionId)
                        .adminId(UUID.randomUUID())
                        .build()
        );

        assertEquals(newSessionId, booking.getTrainingSessionId());
        assertEquals(
                TrialBookingStatus.SCHEDULED,
                booking.getStatus()
        );

        verify(groupPort).validateAvailableCapacity(
                eq(newGroupId),
                eq(booking.getStudentId()),
                any()
        );

        verify(detailsReader).read(booking);
    }

    @Test
    void returnsActiveAndCompletedTrialParticipantsForSession() {
        UUID sessionId = UUID.randomUUID();

        TrialBooking scheduled = createBooking();

        TrialSessionParticipantDto participant =
                TrialSessionParticipantDto.builder()
                        .trialBookingId(scheduled.getId())
                        .fullName("Alex Doe")
                        .bookingStatus(TrialBookingStatus.SCHEDULED)
                        .attendanceStatus(
                                TrialAttendanceStatus.UNMARKED
                        )
                        .build();

        when(repository
                .findAllByTrainingSessionIdAndStatusInOrderByCreatedAtAsc(
                        sessionId,
                        List.of(
                                TrialBookingStatus.SCHEDULED,
                                TrialBookingStatus.COMPLETED
                        )
                ))
                .thenReturn(List.of(scheduled));

        when(sessionParticipantReader.read(List.of(scheduled)))
                .thenReturn(List.of(participant));

        List<TrialSessionParticipantDto> result =
                service.getSessionParticipants(sessionId);

        assertEquals(List.of(participant), result);

        verify(sessionParticipantReader)
                .read(List.of(scheduled));
    }

    @Test
    void recordsCoachRecommendationForAttendedTrial() {
        TrialBooking booking = createBooking();
        UUID coachId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        booking.markAttendance(
                TrialAttendanceStatus.ATTENDED,
                coachId,
                "Посетил"
        );

        TrialSessionParticipantDto output =
                TrialSessionParticipantDto.builder()
                        .trialBookingId(booking.getId())
                        .fullName("Trial Student")
                        .attendanceStatus(
                                TrialAttendanceStatus.ATTENDED
                        )
                        .coachRecommendation(
                                TrialCoachRecommendation
                                        .RECOMMEND_ANOTHER_GROUP
                        )
                        .coachRecommendedGroupId(groupId)
                        .coachRecommendationComment(
                                "Лучше подойдёт старшая группа"
                        )
                        .build();

        when(repository.findLockedById(booking.getId()))
                .thenReturn(Optional.of(booking));

        when(groupPort.getDetails(groupId))
                .thenReturn(
                        TrialBookingDetailsDto.Group.builder()
                                .id(groupId)
                                .name("Older group")
                                .build()
                );

        when(sessionParticipantReader.read(List.of(booking)))
                .thenReturn(List.of(output));

        when(sessionPort.getSessionDetails(booking.getTrainingSessionId())).thenReturn(TrialSessionContext.builder().groupId(groupId).build());
        TrialSessionParticipantDto result =
                service.recordCoachRecommendation(
                        RecordTrialCoachRecommendationCommand.builder().version(0L)
                                .trialId(booking.getId())
                                .coachId(coachId)
                                .recommendation(
                                        TrialCoachRecommendation
                                                .RECOMMEND_ANOTHER_GROUP
                                )
                                .recommendedGroupId(groupId)
                                .comment(
                                        "Лучше подойдёт старшая группа"
                                )
                                .build()
                );

        assertEquals(output, result);

        assertEquals(
                TrialCoachRecommendation.RECOMMEND_ANOTHER_GROUP,
                booking.getCoachRecommendation()
        );
        assertEquals(
                groupId,
                booking.getCoachRecommendedGroupId()
        );
        assertEquals(
                coachId,
                booking.getCoachRecommendationBy()
        );
    }


    @Test void staleAttendanceVersionDoesNotWriteHistory() {
        var booking=createBooking();when(repository.findLockedById(booking.getId())).thenReturn(Optional.of(booking));
        assertThrows(kz.edu.soccerhub.common.exception.ConflictException.class,()->service.markAttendance(MarkTrialAttendanceCommand.builder().trialId(booking.getId()).adminId(UUID.randomUUID()).version(1L).status(TrialAttendanceStatus.ATTENDED).build()));
        assertEquals(TrialAttendanceStatus.UNMARKED,booking.getAttendanceStatus());verify(repository,never()).saveAndFlush(any());org.mockito.Mockito.verifyNoInteractions(journal);
    }
    @Test void versionIsRequiredForEveryAttendanceWriter() {
        assertThrows(BadRequestException.class,()->service.markAttendance(MarkTrialAttendanceCommand.builder().trialId(UUID.randomUUID()).adminId(UUID.randomUUID()).status(TrialAttendanceStatus.ATTENDED).build()));
        org.mockito.Mockito.verifyNoInteractions(repository,journal);
    }
    @Test void futureAttendanceIsRejectedInsideTrialModule() {
        var booking=createBooking();when(repository.findLockedById(booking.getId())).thenReturn(Optional.of(booking));
        when(sessionPort.getSessionDetails(booking.getTrainingSessionId())).thenReturn(sessionContext(booking.getTrainingSessionId(),UUID.randomUUID()));
        assertThrows(BadRequestException.class,()->service.markAttendance(MarkTrialAttendanceCommand.builder().trialId(booking.getId()).adminId(UUID.randomUUID()).version(0L).status(TrialAttendanceStatus.ATTENDED).build()));
        assertEquals(TrialAttendanceStatus.UNMARKED,booking.getAttendanceStatus());org.mockito.Mockito.verifyNoInteractions(journal);
    }
    @Test void pastFollowUpCannotBeSaved() {
        var booking=createBooking();booking.markAttendance(TrialAttendanceStatus.ATTENDED,UUID.randomUUID(),null);when(repository.findLockedById(booking.getId())).thenReturn(Optional.of(booking));
        assertThrows(BadRequestException.class,()->service.recordResult(RecordTrialResultCommand.builder().trialId(booking.getId()).adminId(UUID.randomUUID()).version(0L).result(TrialResult.FOLLOW_UP).nextActionType(kz.edu.soccerhub.trial.domain.enums.TrialNextActionType.CALL).nextActionAt(LocalDateTime.now().minusDays(1)).build()));
        org.mockito.Mockito.verifyNoInteractions(journal);
    }
    @Test void recommendationsRespectCapacityAndBranch() {
        var booking=createBooking();booking.markAttendance(TrialAttendanceStatus.ATTENDED,UUID.randomUUID(),null);var target=UUID.randomUUID();var source=UUID.randomUUID();
        when(repository.findLockedById(booking.getId())).thenReturn(Optional.of(booking));when(sessionPort.getSessionDetails(booking.getTrainingSessionId())).thenReturn(TrialSessionContext.builder().groupId(source).build());
        doThrow(new BadRequestException("foreign branch")).when(groupPort).validateSameBranch(source,target);
        assertThrows(BadRequestException.class,()->service.recordCoachRecommendation(RecordTrialCoachRecommendationCommand.builder().trialId(booking.getId()).coachId(UUID.randomUUID()).version(0L).recommendation(TrialCoachRecommendation.RECOMMEND_ANOTHER_GROUP).recommendedGroupId(target).build()));
        org.mockito.Mockito.verifyNoInteractions(journal);assertEquals(null,booking.getCoachRecommendation());
    }
    @Test void actualConversionLinksOnlyExactParticipantAndClosesFollowUp() {
        var booking=TrialBooking.schedule(UUID.randomUUID(),null,UUID.randomUUID(),null,UUID.randomUUID());var actor=UUID.randomUUID();booking.markAttendance(TrialAttendanceStatus.ATTENDED,actor,null);
        when(repository.findAllByLeadIdAndParticipantIdAndStudentIdIsNull(booking.getLeadId(),booking.getParticipantId())).thenReturn(List.of(booking));when(repository.findLockedById(booking.getId())).thenReturn(Optional.of(booking));
        var student=UUID.randomUUID();service.linkConvertedStudent(LinkTrialStudentCommand.builder().leadId(booking.getLeadId()).participantId(booking.getParticipantId()).clientId(UUID.randomUUID()).studentId(student).actorId(actor).build());
        assertEquals(student,booking.getStudentId());assertEquals(TrialResult.CONVERTED,booking.getResult());assertEquals(null,booking.getNextActionAt());verify(journal).append(eq(booking.getId()),eq(actor),eq("CONVERSION"),any());
    }
    private CreateTrialBookingCommand command(
            UUID leadId,
            UUID studentId,
            UUID sessionId
    ) {
        return CreateTrialBookingCommand.builder()
                .leadId(leadId)
                .participantId(studentId)
                .studentId(studentId)
                .trainingSessionId(sessionId)
                .adminId(UUID.randomUUID())
                .build();
    }

    private TrialSessionContext sessionContext(UUID sessionId, UUID groupId) {
        LocalDateTime startsAt = LocalDateTime.now().plusDays(1);

        return TrialSessionContext.builder()
                .sessionId(sessionId)
                .groupId(groupId)
                .coachId(UUID.randomUUID())
                .startsAt(startsAt)
                .endsAt(startsAt.plusHours(1))
                .status(TrainingSessionStatus.PLANNED)
                .build();
    }

    private TrialBooking createBooking() {
        return TrialBooking.schedule(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID()
        );
    }
    @Test void unconvertedLeadTrialsStillRequireCapacityAndMatchingBranch() {
        var leadId=UUID.randomUUID();var participant=UUID.randomUUID();var sessionId=UUID.randomUUID();var groupId=UUID.randomUUID();var branch=UUID.randomUUID();
        when(sessionPort.getBookableSession(sessionId)).thenReturn(new TrialSessionContext(sessionId,groupId,UUID.randomUUID(),null,java.time.LocalDateTime.now().plusDays(1),java.time.LocalDateTime.now().plusDays(1).plusHours(1),kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus.PLANNED));
        when(groupPort.branchId(groupId)).thenReturn(branch);
        doThrow(new BadRequestException("Нет мест")).when(groupPort).validateAvailableCapacity(eq(groupId),isNull(),any());
        var command=CreateTrialBookingCommand.builder().adminId(UUID.randomUUID()).leadId(leadId).participantId(participant).trainingSessionId(sessionId).build();
        assertThrows(BadRequestException.class,()->service.createTrial(command));verify(leadPort).validateBranch(leadId,branch);verify(repository,never()).save(any());verifyNoInteractions(journal);
    }
    @Test void foreignLeadCannotBeBookedIntoAnotherBranch() {
        var leadId=UUID.randomUUID();var sessionId=UUID.randomUUID();var groupId=UUID.randomUUID();var branch=UUID.randomUUID();
        when(sessionPort.getBookableSession(sessionId)).thenReturn(new TrialSessionContext(sessionId,groupId,UUID.randomUUID(),null,java.time.LocalDateTime.now().plusDays(1),java.time.LocalDateTime.now().plusDays(1).plusHours(1),kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus.PLANNED));
        when(groupPort.branchId(groupId)).thenReturn(branch);doThrow(new BadRequestException("Чужой филиал")).when(leadPort).validateBranch(leadId,branch);
        assertThrows(BadRequestException.class,()->service.createTrial(CreateTrialBookingCommand.builder().adminId(UUID.randomUUID()).leadId(leadId).participantId(UUID.randomUUID()).trainingSessionId(sessionId).build()));verify(repository,never()).save(any());verifyNoInteractions(journal);
    }

}
