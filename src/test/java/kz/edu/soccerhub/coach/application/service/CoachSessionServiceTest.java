package kz.edu.soccerhub.coach.application.service;

import kz.edu.soccerhub.coach.application.dto.session.CoachSessionDetailsResponse;
import kz.edu.soccerhub.coach.application.dto.session.CoachTrialRecommendationInput;
import kz.edu.soccerhub.coach.domain.model.TrainingSession;
import kz.edu.soccerhub.coach.domain.model.enums.TrainingSessionStatus;
import kz.edu.soccerhub.coach.domain.repository.CoachProfileRepository;
import kz.edu.soccerhub.coach.domain.repository.TrainingSessionAttendanceRepository;
import kz.edu.soccerhub.coach.domain.repository.TrainingSessionRepository;
import kz.edu.soccerhub.common.dto.trial.TrialSessionParticipantDto;
import kz.edu.soccerhub.common.port.TrialPort;
import kz.edu.soccerhub.organization.domain.model.Group;
import kz.edu.soccerhub.organization.domain.repository.GroupRepository;
import kz.edu.soccerhub.trial.domain.enums.TrialAttendanceStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialBookingStatus;
import kz.edu.soccerhub.trial.domain.enums.TrialCoachRecommendation;
import kz.edu.soccerhub.trial.domain.enums.TrialResult;
import kz.edu.soccerhub.coach.application.dto.session.CoachSessionTrialStudentItem;
import kz.edu.soccerhub.coach.application.dto.session.CoachTrialAttendanceInput;
import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;
import kz.edu.soccerhub.common.dto.trial.TrialBookingDto;
import kz.edu.soccerhub.common.port.LeadPort;
import kz.edu.soccerhub.crm.application.state.LeadEvent;
import kz.edu.soccerhub.common.exception.ForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoachSessionServiceTest {

    @Mock
    private CoachProfileRepository coachProfileRepository;

    @Mock
    private TrainingSessionRepository trainingSessionRepository;

    @Mock
    private TrainingSessionAttendanceRepository
            trainingSessionAttendanceRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private CoachRosterReader coachRosterReader;

    @Mock
    private TrialPort trialPort;

    @Mock
    private LeadPort leadPort;

    private CoachSessionService service;

    @BeforeEach
    void setUp() {
        service = new CoachSessionService(
                coachProfileRepository,
                trainingSessionRepository,
                trainingSessionAttendanceRepository,
                groupRepository,
                coachRosterReader,
                trialPort,
                leadPort
        );
    }

    @Test
    void returnsTrialStudentsWithoutCrmData() {
        UUID coachId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID trialBookingId = UUID.randomUUID();
        LocalDate sessionDate = LocalDate.now().plusDays(1);

        TrainingSession session = TrainingSession.builder()
                .id(sessionId)
                .groupId(groupId)
                .coachId(coachId)
                .sessionDate(sessionDate)
                .scheduledStartAt(
                        LocalDateTime.of(
                                sessionDate,
                                java.time.LocalTime.of(18, 0)
                        )
                )
                .scheduledEndAt(
                        LocalDateTime.of(
                                sessionDate,
                                java.time.LocalTime.of(19, 0)
                        )
                )
                .status(TrainingSessionStatus.PLANNED)
                .reportDone(false)
                .build();

        Group group = mock(Group.class);

        when(group.getName()).thenReturn("Adal");
        when(coachProfileRepository.existsById(coachId))
                .thenReturn(true);
        when(trainingSessionRepository.findByIdAndCoachId(
                sessionId,
                coachId
        )).thenReturn(Optional.of(session));
        when(groupRepository.findById(groupId))
                .thenReturn(Optional.of(group));
        when(coachRosterReader
                .getActivePlayersByGroupAndDate(
                        groupId,
                        sessionDate
                ))
                .thenReturn(List.of());
        when(trainingSessionAttendanceRepository
                .findBySessionId(sessionId))
                .thenReturn(List.of());

        when(trialPort.getSessionParticipants(sessionId))
                .thenReturn(List.of(
                        TrialSessionParticipantDto.builder()
                                .trialBookingId(trialBookingId)
                                .leadId(UUID.randomUUID())
                                .participantId(UUID.randomUUID())
                                .fullName("Trial Student")
                                .age(9)
                                .bookingStatus(
                                        TrialBookingStatus.SCHEDULED
                                )
                                .attendanceStatus(
                                        TrialAttendanceStatus.UNMARKED
                                )
                                .result(TrialResult.PENDING)
                                .build()
                ));

        CoachSessionDetailsResponse response =
                service.getSessionDetails(
                        coachId,
                        sessionId,
                        "Asia/Almaty"
                );

        assertEquals(0, response.students().size());
        assertEquals("0/0", response.attendanceSummary());
        assertEquals(1, response.trialStudents().size());

        var trialStudent = response.trialStudents().getFirst();

        assertEquals(
                trialBookingId,
                trialStudent.trialBookingId()
        );
        assertEquals("Trial Student", trialStudent.name());
        assertEquals("UNMARKED", trialStudent.attendance());
        assertEquals("PENDING", trialStudent.result());
    }

    @Test
    void marksTrialAttendanceAndUpdatesLinkedLead() {
        UUID coachId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID trialId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        LocalDate sessionDate = LocalDate.now();

        TrainingSession session = TrainingSession.builder()
                .id(sessionId)
                .groupId(groupId)
                .coachId(coachId)
                .sessionDate(sessionDate)
                .scheduledStartAt(
                        LocalDateTime.now().minusMinutes(30)
                )
                .scheduledEndAt(
                        LocalDateTime.now().plusMinutes(30)
                )
                .status(TrainingSessionStatus.IN_PROGRESS)
                .build();

        when(coachProfileRepository.existsById(coachId))
                .thenReturn(true);

        when(trainingSessionRepository.findByIdAndCoachId(
                sessionId,
                coachId
        )).thenReturn(Optional.of(session));

        when(trialPort.getTrial(trialId))
                .thenReturn(
                        TrialBookingDto.builder()
                                .id(trialId)
                                .leadId(leadId)
                                .studentId(studentId)
                                .trainingSessionId(sessionId)
                                .status(TrialBookingStatus.SCHEDULED)
                                .attendanceStatus(
                                        TrialAttendanceStatus.UNMARKED
                                )
                                .result(TrialResult.PENDING)
                                .build()
                );

        when(trialPort.markAttendance(any()))
                .thenReturn(
                        TrialBookingDetailsDto.builder()
                                .id(trialId)
                                .status(TrialBookingStatus.COMPLETED)
                                .attendanceStatus(
                                        TrialAttendanceStatus.ATTENDED
                                )
                                .result(TrialResult.PENDING)
                                .student(
                                        TrialBookingDetailsDto.Student
                                                .builder()
                                                .id(studentId)
                                                .fullName("Trial Student")
                                                .age(9)
                                                .build()
                                )
                                .lead(
                                        TrialBookingDetailsDto.Lead
                                                .builder()
                                                .id(leadId)
                                                .fullName("Parent")
                                                .build()
                                )
                                .attendance(
                                        TrialBookingDetailsDto.Attendance
                                                .builder()
                                                .status(
                                                        TrialAttendanceStatus.ATTENDED
                                                )
                                                .comment("Хорошо работал")
                                                .build()
                                )
                                .outcome(
                                        TrialBookingDetailsDto.Outcome
                                                .builder()
                                                .result(TrialResult.PENDING)
                                                .build()
                                )
                                .build()
                );

        CoachSessionTrialStudentItem result =
                service.markTrialAttendance(
                        coachId,
                        sessionId,
                        trialId,
                        new CoachTrialAttendanceInput(
                                TrialAttendanceStatus.ATTENDED,
                                "Хорошо работал"
                        )
                );

        assertEquals(trialId, result.trialBookingId());
        assertEquals("Trial Student", result.name());
        assertEquals("ATTENDED", result.attendance());
        assertEquals(
                "Хорошо работал",
                result.attendanceComment()
        );

        verify(trialPort).markAttendance(argThat(command ->
                trialId.equals(command.trialId())
                        && coachId.equals(command.adminId())
                        && command.status()
                        == TrialAttendanceStatus.ATTENDED
        ));

        verify(leadPort).processEvent(
                leadId,
                LeadEvent.COMPLETE_TRIAL,
                null,
                null,
                null
        );
    }

    @Test
    void rejectsTrialAttendanceFromAnotherSession() {
        UUID coachId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID anotherSessionId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID trialId = UUID.randomUUID();
        LocalDate sessionDate = LocalDate.now();

        TrainingSession session = TrainingSession.builder()
                .id(sessionId)
                .groupId(groupId)
                .coachId(coachId)
                .sessionDate(sessionDate)
                .scheduledStartAt(
                        LocalDateTime.now().minusMinutes(30)
                )
                .scheduledEndAt(
                        LocalDateTime.now().plusMinutes(30)
                )
                .status(TrainingSessionStatus.IN_PROGRESS)
                .build();

        when(coachProfileRepository.existsById(coachId))
                .thenReturn(true);

        when(trainingSessionRepository.findByIdAndCoachId(
                sessionId,
                coachId
        )).thenReturn(Optional.of(session));

        when(trialPort.getTrial(trialId))
                .thenReturn(
                        TrialBookingDto.builder()
                                .id(trialId)
                                .trainingSessionId(anotherSessionId)
                                .status(TrialBookingStatus.SCHEDULED)
                                .attendanceStatus(
                                        TrialAttendanceStatus.UNMARKED
                                )
                                .result(TrialResult.PENDING)
                                .build()
                );

        assertThrows(
                ForbiddenException.class,
                () -> service.markTrialAttendance(
                        coachId,
                        sessionId,
                        trialId,
                        new CoachTrialAttendanceInput(
                                TrialAttendanceStatus.ATTENDED,
                                null
                        )
                )
        );

        verify(trialPort, never())
                .markAttendance(any());

        verifyNoInteractions(leadPort);
    }

    @Test
    void recordsRecommendationForOwnSessionTrial() {
        UUID coachId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID trialId = UUID.randomUUID();
        UUID recommendedGroupId = UUID.randomUUID();
        LocalDate sessionDate = LocalDate.now();

        TrainingSession session = TrainingSession.builder()
                .id(sessionId)
                .groupId(groupId)
                .coachId(coachId)
                .sessionDate(sessionDate)
                .scheduledStartAt(
                        LocalDateTime.now().minusHours(2)
                )
                .scheduledEndAt(
                        LocalDateTime.now().minusHours(1)
                )
                .status(TrainingSessionStatus.COMPLETED)
                .build();

        when(coachProfileRepository.existsById(coachId))
                .thenReturn(true);

        when(trainingSessionRepository.findByIdAndCoachId(
                sessionId,
                coachId
        )).thenReturn(Optional.of(session));

        when(trialPort.getTrial(trialId))
                .thenReturn(
                        TrialBookingDto.builder()
                                .id(trialId)
                                .trainingSessionId(sessionId)
                                .status(TrialBookingStatus.COMPLETED)
                                .attendanceStatus(
                                        TrialAttendanceStatus.ATTENDED
                                )
                                .result(TrialResult.PENDING)
                                .build()
                );

        TrialSessionParticipantDto updated =
                TrialSessionParticipantDto.builder()
                        .trialBookingId(trialId)
                        .fullName("Trial Student")
                        .age(9)
                        .bookingStatus(
                                TrialBookingStatus.COMPLETED
                        )
                        .attendanceStatus(
                                TrialAttendanceStatus.ATTENDED
                        )
                        .result(TrialResult.PENDING)
                        .coachRecommendation(
                                TrialCoachRecommendation
                                        .RECOMMEND_ANOTHER_GROUP
                        )
                        .coachRecommendedGroupId(
                                recommendedGroupId
                        )
                        .coachRecommendationComment(
                                "Лучше подойдёт другая группа"
                        )
                        .coachRecommendationAt(
                                LocalDateTime.now()
                        )
                        .build();

        when(trialPort.recordCoachRecommendation(any()))
                .thenReturn(updated);

        CoachSessionTrialStudentItem result =
                service.recordTrialRecommendation(
                        coachId,
                        sessionId,
                        trialId,
                        new CoachTrialRecommendationInput(
                                TrialCoachRecommendation
                                        .RECOMMEND_ANOTHER_GROUP,
                                recommendedGroupId,
                                "Лучше подойдёт другая группа"
                        )
                );

        assertEquals(
                "RECOMMEND_ANOTHER_GROUP",
                result.coachRecommendation()
        );
        assertEquals(
                recommendedGroupId,
                result.coachRecommendedGroupId()
        );
        assertEquals(
                "Лучше подойдёт другая группа",
                result.coachRecommendationComment()
        );

        verify(trialPort).recordCoachRecommendation(
                argThat(command ->
                        trialId.equals(command.trialId())
                                && coachId.equals(command.coachId())
                                && command.recommendation()
                                == TrialCoachRecommendation
                                .RECOMMEND_ANOTHER_GROUP
                                && recommendedGroupId.equals(
                                command.recommendedGroupId()
                        )
                )
        );

        verifyNoInteractions(leadPort);
    }
}