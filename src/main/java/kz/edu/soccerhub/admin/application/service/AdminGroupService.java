package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.group.AdminCancelScheduleBatchInput;
import kz.edu.soccerhub.admin.application.dto.group.AdminGroupCoachOutput;
import kz.edu.soccerhub.admin.application.dto.group.AdminGroupCreateInput;
import kz.edu.soccerhub.common.dto.coach.CoachDto;
import kz.edu.soccerhub.common.dto.group.*;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.*;
import kz.edu.soccerhub.organization.application.dto.CoachBusySlotView;
import kz.edu.soccerhub.organization.domain.model.enums.CoachRole;
import kz.edu.soccerhub.organization.domain.model.enums.GroupStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminGroupService {

    private final GroupPort groupPort;
    private final CoachPort coachPort;
    private final GroupCoachPort groupCoachPort;
    private final GroupSchedulePort groupSchedulePort;
    private final CoachAvailabilityPort coachAvailabilityPort;

    private final AdminService adminService;
    private final AdminBranchService adminBranchService;

    /* ================= GROUP ================= */

    @Transactional
    public UUID createGroup(UUID adminId, AdminGroupCreateInput input) {
        verifyAdmin(adminId);
        verifyAdminBranchAccess(adminId, input.branchId());

        CreateGroupCommand command = CreateGroupCommand.builder()
                .name(input.name())
                .description(input.description())
                .ageFrom(input.ageFrom())
                .ageTo(input.ageTo())
                .capacity(input.capacity())
                .level(input.level())
                .branchId(input.branchId())
                .build();

        UUID groupId = groupPort.createGroup(command);

        log.info(
                "Admin {} created group {} in branch {}",
                adminId, groupId, input.branchId()
        );

        return groupId;
    }

    @Transactional
    public void updateGroupStatus(UUID adminId, UUID groupId, GroupStatus status) {
        verifyAdmin(adminId);
        verifyAdminBranchAccess(adminId, groupPort.getGroupById(groupId).branchId());

        groupPort.updateStatus(groupId, status);

        log.info(
                "Admin {} updated group status {} to {}",
                adminId, groupId, status
        );
    }

    /* ================= GROUP COACH ================= */

    @Transactional
    public UUID assignCoachToGroup(UUID adminId, UUID groupId, UUID coachId, CoachRole role) {
        verifyAdmin(adminId);
        verifyAdminBranchAccess(adminId, groupPort.getGroupById(groupId).branchId());
        verifyCoach(coachId);

        UUID assignmentId = groupCoachPort.assignCoach(groupId, coachId, role);

        log.info(
                "Admin {} assigned coach {} to group {}",
                adminId, coachId, groupId
        );

        return assignmentId;
    }

    @Transactional
    public void unassignCoachFromGroup(UUID adminId, UUID groupCoachId) {

        verifyAdmin(adminId);

        groupCoachPort.unassignCoach(groupCoachId);

        log.info(
                "Admin {} unassigned coach from group {}",
                adminId, groupCoachId
        );
    }

    @Transactional(readOnly = true)
    public Collection<AdminGroupCoachOutput> getGroupCoaches(UUID groupId) {
        log.debug("======= Fetching group -> {} coaches =======", groupId);

        Collection<GroupCoachDto> activeGroupCoaches =
                groupCoachPort.getActiveCoaches(groupId);

        log.debug("======= Group {} coaches ids {} =======", groupId, activeGroupCoaches);

        Collection<CoachDto> coaches = coachPort.getCoaches(
                activeGroupCoaches.stream()
                        .map(GroupCoachDto::coachId)
                        .collect(Collectors.toSet())
        );

        log.debug("======= Merge group coach and coach info =======");

        Map<UUID, CoachDto> coachMap = coaches.stream()
                .collect(Collectors.toMap(CoachDto::id, Function.identity()));

        return activeGroupCoaches.stream()
                .map(groupCoach -> {
                    CoachDto coach = coachMap.get(groupCoach.coachId());

                    if (coach == null) {
                        log.warn("Coach not found for coachId={}", groupCoach.coachId());
                        return null;
                    }

                    return AdminGroupCoachOutput.builder()
                            .groupCoachId(groupCoach.id())
                            .coachId(coach.id())
                            .groupId(groupCoach.groupId())
                            .coachFirstName(coach.firstName())
                            .coachLastName(coach.lastName())
                            .birthDate(coach.birthDate())
                            .phone(coach.phone())
                            .email(coach.email())
                            .active(coach.active())
                            .coachRole(groupCoach.role())
                            .assignedFrom(groupCoach.assignedFrom())
                            .assignedTo(groupCoach.assignedTo())
                            .createdAt(groupCoach.createdAt())
                            .updatedAt(groupCoach.updateAt())
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /* ================= SCHEDULE ================= */

    @Transactional
    public void createGroupSchedule(
            UUID adminId,
            UUID groupId,
            GroupScheduleBatchCommand command
    ) {
        verifyAdmin(adminId);
        verifyAdminBranchAccess(adminId, groupPort.getGroupById(groupId).branchId());

        groupSchedulePort.createSchedule(groupId, command);

        log.info(
                "Admin {} created schedule for group {}",
                adminId, groupId
        );
    }

    @Transactional
    public void cancelSchedule(UUID adminId, UUID scheduleId) {
        verifyAdmin(adminId);

        groupSchedulePort.cancelSchedule(scheduleId);

        log.info(
                "Admin {} cancelled schedule {}",
                adminId, scheduleId
        );
    }

    @Transactional
    public void cancelScheduleBatch(UUID adminId, UUID groupId, AdminCancelScheduleBatchInput input) {
        verifyAdmin(adminId);

        CancelScheduleBatchCommand command = CancelScheduleBatchCommand.builder()
                .coachId(input.coachId())
                .startDate(input.startDate())
                .endDate(input.endDate())
                .type(input.type())
                .build();

        groupSchedulePort.cancelScheduleBatch(groupId, command);

        log.info("Cancelled schedule batch for group {} by admin {}", groupId, adminId);
    }

    @Transactional
    public void updateGroupSchedule(
            UUID adminId,
            UUID groupId,
            UpdateScheduleBatchCommand command
    ) {
        verifyAdmin(adminId);
        verifyAdminBranchAccess(adminId, groupPort.getGroupById(groupId).branchId());

        groupSchedulePort.updateScheduleBatch(groupId, command);

        log.info(
                "Admin {} updated schedule for group {}",
                adminId, groupId
        );
    }

    /* ================= AVAILABILITY ================= */

    @Transactional(readOnly = true)
    public List<CoachBusySlotView> getCoachAvailability(
            UUID adminId,
            UUID coachId,
            LocalDate from,
            LocalDate to
    ) {
        verifyAdmin(adminId);

        return coachAvailabilityPort.getCoachAvailability(coachId, from, to);
    }

    /* ================= INTERNAL ================= */

    private void verifyAdmin(UUID adminId) {
        adminService.findById(adminId)
                .orElseThrow(() ->
                        new NotFoundException("Admin not found", adminId)
                );
    }

    private void verifyAdminBranchAccess(UUID adminId, UUID branchId) {
        boolean hasAccess =
                adminBranchService.verifyAdminBelongsToBranch(adminId, branchId);

        if (!hasAccess) {
            throw new BadRequestException(
                    "Admin does not have access to branch",
                    branchId
            );
        }
    }

    private void verifyCoach(UUID coachId) {
        if (!coachPort.verifyCoach(coachId)) {
            throw new NotFoundException("Coach not found", coachId);
        }
    }
}