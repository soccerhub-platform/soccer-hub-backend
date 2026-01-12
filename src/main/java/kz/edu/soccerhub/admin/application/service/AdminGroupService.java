package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.AdminGroupCreateInput;
import kz.edu.soccerhub.common.dto.group.CreateGroupCommand;
import kz.edu.soccerhub.common.dto.group.GroupScheduleBatchCommand;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.*;
import kz.edu.soccerhub.organization.application.dto.CoachBusySlotView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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

    /* ================= GROUP COACH ================= */

    @Transactional
    public UUID assignCoachToGroup(UUID adminId, UUID groupId, UUID coachId) {
        verifyAdmin(adminId);
        verifyAdminBranchAccess(adminId, groupPort.getGroupById(groupId).branchId());
        verifyCoach(coachId);

        UUID assignmentId = groupCoachPort.assignCoach(groupId, coachId);

        log.info(
                "Admin {} assigned coach {} to group {}",
                adminId, coachId, groupId
        );

        return assignmentId;
    }

    @Transactional
    public void unassignCoachFromGroup(UUID adminId, UUID groupId, UUID coachId) {

        verifyAdmin(adminId);
        verifyAdminBranchAccess(adminId, groupPort.getGroupById(groupId).branchId());
        verifyCoach(coachId);

        groupCoachPort.unassignCoach(groupId, coachId);

        log.info(
                "Admin {} unassigned coach {} from group {}",
                adminId, coachId, groupId
        );
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