package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.group.AdminAddGroupMemberInput;
import kz.edu.soccerhub.admin.application.dto.group.AdminGroupMemberCandidatesOutput;
import kz.edu.soccerhub.admin.application.dto.group.AdminGroupMembershipOutput;
import kz.edu.soccerhub.admin.application.dto.group.AdminGroupMembershipTransferOutput;
import kz.edu.soccerhub.admin.application.dto.group.AdminRemoveGroupMembershipInput;
import kz.edu.soccerhub.admin.application.dto.group.AdminTransferGroupMembershipInput;
import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.common.dto.media.MediaAssetResponse;
import kz.edu.soccerhub.common.dto.student.StudentProfileDto;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.ConflictException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.ClientPort;
import kz.edu.soccerhub.common.port.GroupActivityPort;
import kz.edu.soccerhub.common.port.GroupMembershipPort;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.common.port.MediaAccessPort;
import kz.edu.soccerhub.common.port.MediaAvatarPort;
import kz.edu.soccerhub.organization.domain.model.enums.GroupAudienceType;
import kz.edu.soccerhub.organization.domain.model.GroupMembership;
import kz.edu.soccerhub.organization.domain.model.enums.GroupMembershipStatus;
import kz.edu.soccerhub.organization.domain.model.enums.GroupStatus;
import kz.edu.soccerhub.media.domain.enums.MediaOwnerType;
import kz.edu.soccerhub.media.domain.model.MediaAsset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminGroupMembershipService {

    private static final String ACTIVITY_STUDENT_ADDED = "STUDENT_ADDED";
    private static final String ACTIVITY_STUDENT_TRANSFERRED = "STUDENT_TRANSFERRED";
    private static final String ACTIVITY_STUDENT_REMOVED = "STUDENT_REMOVED";

    private final GroupMembershipPort groupMembershipPort;
    private final GroupPort groupPort;
    private final ClientPort clientPort;
    private final AdminService adminService;
    private final AdminBranchService adminBranchService;
    private final GroupActivityPort groupActivityPort;
    private final MediaAvatarPort mediaAvatarPort;
    private final MediaAccessPort mediaAccessPort;

    @Transactional(readOnly = true)
    public AdminGroupMemberCandidatesOutput getMemberCandidates(UUID adminId, UUID groupId, String search, int page, int size) {
        verifyAdmin(adminId);

        GroupDto group = groupPort.getGroupById(groupId);
        verifyAdminBranchAccess(adminId, group.branchId());

        List<StudentProfileDto> profiles = clientPort.getStudentProfilesByBranch(group.branchId());
        if (profiles.isEmpty()) {
            return new AdminGroupMemberCandidatesOutput(groupId, List.of(), 0, Math.max(page, 0), Math.max(size, 1));
        }

        String normalizedSearch = trimToNull(search);
        LocalDate today = LocalDate.now();
        Map<UUID, List<GroupMembership>> membershipsByPlayerId = groupMembershipPort
                .findActiveByPlayerIdInAsOfDate(
                        profiles.stream().map(StudentProfileDto::playerId).toList(),
                        today
                ).stream()
                .collect(Collectors.groupingBy(GroupMembership::getPlayerId));
        Set<UUID> relatedGroupIds = membershipsByPlayerId.values().stream()
                .flatMap(List::stream)
                .map(GroupMembership::getGroupId)
                .collect(Collectors.toSet());
        Map<UUID, GroupMembership> recentTargetMembershipsByPlayerId = groupMembershipPort
                .findByGroupIdAndPlayerIdInEndingOnOrAfterDate(
                        groupId,
                        profiles.stream().map(StudentProfileDto::playerId).toList(),
                        today
                ).stream()
                .collect(Collectors.toMap(
                        GroupMembership::getPlayerId,
                        membership -> membership,
                        this::pickLaterMembership
                ));
        Map<UUID, GroupDto> groupsById = groupPort.getGroupsByIds(relatedGroupIds).stream()
                .collect(Collectors.toMap(GroupDto::groupId, value -> value));
        Map<UUID, MediaAsset> groupAvatarsById = mediaAvatarPort.findActiveAvatars(
                MediaOwnerType.GROUP,
                relatedGroupIds
        );
        if (groupAvatarsById == null) {
            groupAvatarsById = Map.of();
        }
        Map<UUID, MediaAsset> groupAvatars = groupAvatarsById;

        List<AdminGroupMemberCandidatesOutput.Item> items = profiles.stream()
                .filter(profile -> matchesSearch(profile, normalizedSearch))
                .filter(profile -> !hasActiveMembershipInGroup(membershipsByPlayerId.get(profile.playerId()), groupId))
                .sorted(Comparator.comparing(StudentProfileDto::playerFullName, String.CASE_INSENSITIVE_ORDER))
                .map(profile -> toCandidate(
                        group,
                        profile,
                        membershipsByPlayerId.get(profile.playerId()),
                        recentTargetMembershipsByPlayerId.get(profile.playerId()),
                        groupsById,
                        groupAvatars
                ))
                .toList();

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        int fromIndex = Math.min(safePage * safeSize, items.size());
        int toIndex = Math.min(fromIndex + safeSize, items.size());

        return new AdminGroupMemberCandidatesOutput(
                groupId,
                items.subList(fromIndex, toIndex),
                items.size(),
                safePage,
                safeSize
        );
    }

    @Transactional
    public AdminGroupMembershipOutput addMember(UUID adminId, UUID groupId, AdminAddGroupMemberInput input) {
        verifyAdmin(adminId);

        GroupDto lockedGroup = groupPort.getGroupByIdForUpdate(groupId);
        verifyAdminBranchAccess(adminId, lockedGroup.branchId());
        validateTargetGroup(lockedGroup);

        StudentProfileDto player = findPlayerProfile(input.playerId());
        verifyPlayerBranchMatchesGroup(player, lockedGroup.branchId());

        Optional<GroupMembership> overlappingMembership = groupMembershipPort.findByGroupIdAndPlayerIdAsOfDate(
                groupId,
                input.playerId(),
                input.joinedAt()
        );
        if (overlappingMembership.isPresent()) {
            Map<String, Object> metadata = buildMembershipOverlapMetadata(groupId, input.playerId(), overlappingMembership.get());
            throw new ConflictException(
                    "Player already participates in this group on selected date",
                    "MEMBERSHIP_DATE_OVERLAP",
                    metadata
            );
        }

        ensureCapacityAvailable(lockedGroup, input.joinedAt());

        GroupMembership membership = groupMembershipPort.save(GroupMembership.builder()
                .groupId(groupId)
                .playerId(input.playerId())
                .status(resolveInitialStatus(input.joinedAt()))
                .joinedAt(input.joinedAt())
                .joinReason(trimToNull(input.reason()))
                .comment(trimToNull(input.comment()))
                .build());

        groupActivityPort.recordGroupActivity(
                groupId,
                adminId,
                ACTIVITY_STUDENT_ADDED,
                activityPayload()
                        .put("membershipId", membership.getId())
                        .put("playerId", player.playerId())
                        .put("playerName", player.playerFullName())
                        .put("joinedAt", membership.getJoinedAt())
                        .build()
        );

        return toOutput(membership, lockedGroup.name(), getGroupAvatar(lockedGroup.groupId()), player);
    }

    @Transactional
    public AdminGroupMembershipTransferOutput transferMember(UUID adminId, UUID membershipId, AdminTransferGroupMembershipInput input) {
        verifyAdmin(adminId);

        GroupMembership currentMembership = groupMembershipPort.findByIdForUpdate(membershipId)
                .orElseThrow(() -> new NotFoundException("Group membership not found", membershipId));

        if (currentMembership.getStatus() != GroupMembershipStatus.ACTIVE && currentMembership.getStatus() != GroupMembershipStatus.UPCOMING) {
            throw new ConflictException(
                    "Only active or upcoming group memberships can be transferred",
                    "GROUP_MEMBERSHIP_NOT_TRANSFERABLE",
                    Map.of("membershipId", membershipId, "status", currentMembership.getStatus().name())
            );
        }
        if (currentMembership.getGroupId().equals(input.targetGroupId())) {
            throw new BadRequestException("Target group must be different from current group", input.targetGroupId());
        }
        if (!input.transferDate().isAfter(currentMembership.getJoinedAt())) {
            throw new BadRequestException("Transfer date must be after membership joinedAt", input.transferDate(), currentMembership.getJoinedAt());
        }

        GroupDto sourceGroup = groupPort.getGroupById(currentMembership.getGroupId());
        verifyAdminBranchAccess(adminId, sourceGroup.branchId());

        GroupDto targetGroup = groupPort.getGroupByIdForUpdate(input.targetGroupId());
        verifyAdminBranchAccess(adminId, targetGroup.branchId());
        validateTargetGroup(targetGroup);

        StudentProfileDto player = findPlayerProfile(currentMembership.getPlayerId());
        verifyPlayerBranchMatchesGroup(player, targetGroup.branchId());

        if (groupMembershipPort.existsActiveByGroupIdAndPlayerIdAsOfDate(targetGroup.groupId(), currentMembership.getPlayerId(), input.transferDate())) {
            throw new ConflictException(
                    "Player already participates in target group on transfer date",
                    "GROUP_MEMBERSHIP_TARGET_ALREADY_ACTIVE",
                    Map.of("targetGroupId", targetGroup.groupId(), "playerId", currentMembership.getPlayerId(), "transferDate", input.transferDate())
            );
        }

        ensureCapacityAvailable(targetGroup, input.transferDate());

        currentMembership.setStatus(GroupMembershipStatus.TRANSFERRED);
        currentMembership.setLeftAt(input.transferDate().minusDays(1));
        currentMembership.setLeaveReason(trimToNull(input.reason()));
        currentMembership.setComment(trimToNull(input.comment()));

        GroupMembership newMembership = groupMembershipPort.save(GroupMembership.builder()
                .groupId(targetGroup.groupId())
                .playerId(currentMembership.getPlayerId())
                .status(resolveInitialStatus(input.transferDate()))
                .joinedAt(input.transferDate())
                .joinReason(trimToNull(input.reason()))
                .comment(trimToNull(input.comment()))
                .build());

        UUID correlationId = UUID.randomUUID();
        Map<String, Object> transferPayload = activityPayload()
                .put("previousMembershipId", currentMembership.getId())
                .put("newMembershipId", newMembership.getId())
                .put("playerId", player.playerId())
                .put("playerName", player.playerFullName())
                .put("sourceGroupId", sourceGroup.groupId())
                .put("sourceGroupName", sourceGroup.name())
                .put("targetGroupId", targetGroup.groupId())
                .put("targetGroupName", targetGroup.name())
                .put("transferDate", input.transferDate())
                .put("reason", trimToNull(input.reason()))
                .build();
        groupActivityPort.recordGroupActivity(
                sourceGroup.groupId(),
                adminId,
                ACTIVITY_STUDENT_TRANSFERRED,
                withDirection(transferPayload, "OUT"),
                correlationId
        );
        groupActivityPort.recordGroupActivity(
                targetGroup.groupId(),
                adminId,
                ACTIVITY_STUDENT_TRANSFERRED,
                withDirection(transferPayload, "IN"),
                correlationId
        );

        return new AdminGroupMembershipTransferOutput(
                toOutput(currentMembership, sourceGroup.name(), getGroupAvatar(sourceGroup.groupId()), player),
                toOutput(newMembership, targetGroup.name(), getGroupAvatar(targetGroup.groupId()), player)
        );
    }

    @Transactional
    public AdminGroupMembershipOutput removeMember(UUID adminId, UUID membershipId, AdminRemoveGroupMembershipInput input) {
        verifyAdmin(adminId);

        GroupMembership membership = groupMembershipPort.findByIdForUpdate(membershipId)
                .orElseThrow(() -> new NotFoundException("Group membership not found", membershipId));

        if (membership.getStatus() != GroupMembershipStatus.ACTIVE && membership.getStatus() != GroupMembershipStatus.UPCOMING) {
            throw new ConflictException(
                    "Only active or upcoming group memberships can be removed",
                    "GROUP_MEMBERSHIP_NOT_REMOVABLE",
                    Map.of("membershipId", membershipId, "status", membership.getStatus().name())
            );
        }
        if (input.leftAt().isBefore(membership.getJoinedAt())) {
            throw new BadRequestException("Membership leftAt cannot be before joinedAt", input.leftAt(), membership.getJoinedAt());
        }

        GroupDto group = groupPort.getGroupById(membership.getGroupId());
        verifyAdminBranchAccess(adminId, group.branchId());

        StudentProfileDto player = findPlayerProfile(membership.getPlayerId());

        membership.setStatus(GroupMembershipStatus.REMOVED);
        membership.setLeftAt(input.leftAt());
        membership.setLeaveReason(trimToNull(input.reason()));
        membership.setComment(trimToNull(input.comment()));

        groupActivityPort.recordGroupActivity(
                membership.getGroupId(),
                adminId,
                ACTIVITY_STUDENT_REMOVED,
                activityPayload()
                        .put("membershipId", membership.getId())
                        .put("playerId", player.playerId())
                        .put("playerName", player.playerFullName())
                        .put("leftAt", membership.getLeftAt())
                        .put("reason", trimToNull(input.reason()))
                        .build()
        );

        return toOutput(membership, group.name(), getGroupAvatar(group.groupId()), player);
    }

    private Map<String, Object> withDirection(Map<String, Object> payload, String direction) {
        java.util.LinkedHashMap<String, Object> result = new java.util.LinkedHashMap<>(payload);
        result.put("direction", direction);
        return result;
    }

    private ActivityPayloadBuilder activityPayload() {
        return new ActivityPayloadBuilder();
    }

    private static final class ActivityPayloadBuilder {
        private final java.util.LinkedHashMap<String, Object> values = new java.util.LinkedHashMap<>();

        private ActivityPayloadBuilder put(String key, Object value) {
            values.put(key, value == null ? null : value.toString());
            return this;
        }

        private Map<String, Object> build() {
            return values;
        }
    }

    private void ensureCapacityAvailable(GroupDto group, LocalDate asOfDate) {
        long activeCount = groupMembershipPort.countActiveByGroupIdAsOfDate(group.groupId(), asOfDate);
        if (group.capacity() != null && activeCount >= group.capacity()) {
            throw new ConflictException(
                    "Group capacity exceeded",
                    "GROUP_CAPACITY_EXCEEDED",
                    Map.of("groupId", group.groupId(), "capacity", group.capacity(), "activeCount", activeCount, "asOfDate", asOfDate)
            );
        }
    }

    private void validateTargetGroup(GroupDto group) {
        if (group.status() != GroupStatus.ACTIVE) {
            throw new ConflictException(
                    "Group must be active for membership changes",
                    "GROUP_NOT_ACTIVE",
                    Map.of("groupId", group.groupId(), "status", group.status().name())
            );
        }
    }

    private StudentProfileDto findPlayerProfile(UUID playerId) {
        return clientPort.getStudentProfile(playerId);
    }

    private void verifyPlayerBranchMatchesGroup(StudentProfileDto player, UUID branchId) {
        if (player.branchId() == null || !player.branchId().equals(branchId)) {
            throw new ConflictException(
                    "Player belongs to another branch",
                    "PLAYER_BRANCH_MISMATCH",
                    Map.of("playerId", player.playerId(), "playerBranchId", player.branchId(), "groupBranchId", branchId)
            );
        }
    }

    private GroupMembershipStatus resolveInitialStatus(LocalDate joinedAt) {
        return joinedAt.isAfter(LocalDate.now()) ? GroupMembershipStatus.UPCOMING : GroupMembershipStatus.ACTIVE;
    }

    private AdminGroupMemberCandidatesOutput.Item toCandidate(
            GroupDto targetGroup,
            StudentProfileDto profile,
            List<GroupMembership> activeMemberships,
            GroupMembership recentTargetMembership,
            Map<UUID, GroupDto> groupsById,
            Map<UUID, MediaAsset> groupAvatarsById
    ) {
        Integer age = calculateAge(profile.birthDate());
        LocalDate earliestAvailableJoinDate = resolveEarliestAvailableJoinDate(recentTargetMembership);
        boolean eligible = recentTargetMembership == null || recentTargetMembership.getLeftAt() != null;
        List<AdminGroupMemberCandidatesOutput.Warning> warnings = buildWarnings(
                targetGroup,
                age,
                earliestAvailableJoinDate,
                recentTargetMembership
        );
        List<AdminGroupMemberCandidatesOutput.CurrentMembership> currentMemberships = activeMemberships == null
                ? List.of()
                : activeMemberships.stream()
                .sorted(Comparator.comparing(GroupMembership::getJoinedAt).reversed())
                .map(membership -> new AdminGroupMemberCandidatesOutput.CurrentMembership(
                        membership.getId(),
                        membership.getGroupId(),
                        resolveGroupName(groupsById, membership.getGroupId()),
                        toMediaAssetResponse(groupAvatarsById.get(membership.getGroupId())),
                        membership.getStatus().name(),
                        membership.getJoinedAt(),
                        membership.getLeftAt()
                ))
                .toList();

        return new AdminGroupMemberCandidatesOutput.Item(
                profile.playerId(),
                profile.playerFullName(),
                profile.birthDate(),
                age,
                eligible,
                earliestAvailableJoinDate,
                warnings,
                currentMemberships
        );
    }

    private List<AdminGroupMemberCandidatesOutput.Warning> buildWarnings(
            GroupDto group,
            Integer age,
            LocalDate earliestAvailableJoinDate,
            GroupMembership recentTargetMembership
    ) {
        List<AdminGroupMemberCandidatesOutput.Warning> warnings = new java.util.ArrayList<>();
        if (group.audienceType() != GroupAudienceType.CHILDREN || age == null) {
        } else if (group.ageFrom() != null && age < group.ageFrom()) {
            warnings.add(new AdminGroupMemberCandidatesOutput.Warning(
                    "PLAYER_AGE_OUTSIDE_GROUP_RANGE",
                    "Возраст ученика ниже рекомендуемого для группы"
            ));
        } else if (group.ageTo() != null && age > group.ageTo()) {
            warnings.add(new AdminGroupMemberCandidatesOutput.Warning(
                    "PLAYER_AGE_OUTSIDE_GROUP_RANGE",
                    "Возраст ученика выше рекомендуемого для группы"
            ));
        }
        if (earliestAvailableJoinDate != null) {
            warnings.add(new AdminGroupMemberCandidatesOutput.Warning(
                    "MEMBERSHIP_AVAILABLE_FROM",
                    "Повторное добавление возможно с " + earliestAvailableJoinDate
            ));
        } else if (recentTargetMembership != null && recentTargetMembership.getLeftAt() == null) {
            warnings.add(new AdminGroupMemberCandidatesOutput.Warning(
                    "PLAYER_ALREADY_IN_GROUP",
                    "Ученик уже состоит в этой группе"
            ));
        }
        return List.copyOf(warnings);
    }

    private boolean hasActiveMembershipInGroup(List<GroupMembership> memberships, UUID groupId) {
        return memberships != null && memberships.stream().anyMatch(item -> groupId.equals(item.getGroupId()));
    }

    private LocalDate resolveEarliestAvailableJoinDate(GroupMembership membership) {
        if (membership == null || membership.getLeftAt() == null) {
            return null;
        }
        LocalDate nextDate = membership.getLeftAt().plusDays(1);
        return nextDate.isAfter(LocalDate.now()) ? nextDate : null;
    }

    private Map<String, Object> buildMembershipOverlapMetadata(UUID groupId, UUID playerId, GroupMembership membership) {
        java.util.LinkedHashMap<String, Object> metadata = new java.util.LinkedHashMap<>();
        metadata.put("groupId", groupId);
        metadata.put("playerId", playerId);
        metadata.put("membershipId", membership.getId());
        metadata.put("membershipStatus", membership.getStatus().name());
        metadata.put("joinedAt", membership.getJoinedAt());
        metadata.put("leftAt", membership.getLeftAt());
        if (membership.getLeftAt() != null) {
            metadata.put("earliestAvailableJoinDate", membership.getLeftAt().plusDays(1));
        }
        return metadata;
    }

    private GroupMembership pickLaterMembership(GroupMembership left, GroupMembership right) {
        LocalDate leftBoundary = left.getLeftAt() == null ? LocalDate.MAX : left.getLeftAt();
        LocalDate rightBoundary = right.getLeftAt() == null ? LocalDate.MAX : right.getLeftAt();
        int boundaryComparison = leftBoundary.compareTo(rightBoundary);
        if (boundaryComparison != 0) {
            return boundaryComparison >= 0 ? left : right;
        }
        return left.getJoinedAt().compareTo(right.getJoinedAt()) >= 0 ? left : right;
    }

    private boolean matchesSearch(StudentProfileDto profile, String search) {
        if (search == null) {
            return true;
        }
        String normalized = search.toLowerCase();
        return contains(profile.playerFullName(), normalized)
                || contains(profile.clientFullName(), normalized)
                || contains(profile.phone(), normalized)
                || contains(profile.email(), normalized)
                || profile.playerId().toString().equalsIgnoreCase(search);
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase().contains(search);
    }

    private String resolveGroupName(Map<UUID, GroupDto> groupsById, UUID groupId) {
        GroupDto group = groupsById.get(groupId);
        return group == null ? null : group.name();
    }

    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    private AdminGroupMembershipOutput toOutput(
            GroupMembership membership,
            String groupName,
            MediaAssetResponse groupAvatar,
            StudentProfileDto player
    ) {
        return new AdminGroupMembershipOutput(
                membership.getId(),
                new AdminGroupMembershipOutput.GroupRef(membership.getGroupId(), groupName, groupAvatar),
                new AdminGroupMembershipOutput.PlayerRef(player.playerId(), player.playerFullName(), player.birthDate()),
                membership.getStatus().name(),
                membership.getJoinedAt(),
                membership.getLeftAt(),
                membership.getJoinReason(),
                membership.getLeaveReason(),
                membership.getComment(),
                membership.getSourceContractId()
        );
    }

    private MediaAssetResponse getGroupAvatar(UUID groupId) {
        Optional<MediaAsset> avatar = mediaAvatarPort.findActiveAvatar(MediaOwnerType.GROUP, groupId);
        return toMediaAssetResponse(avatar == null ? null : avatar.orElse(null));
    }

    private MediaAssetResponse toMediaAssetResponse(MediaAsset asset) {
        return asset == null ? null : mediaAccessPort.toResponse(asset);
    }

    private String joinName(String firstName, String lastName) {
        String left = firstName == null ? "" : firstName.trim();
        String right = lastName == null ? "" : lastName.trim();
        return (left + " " + right).trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void verifyAdmin(UUID adminId) {
        Optional<AdminDto> adminOpt = adminService.findById(adminId);
        if (adminOpt.isEmpty()) {
            throw new NotFoundException("Admin not found", adminId);
        }
    }

    private void verifyAdminBranchAccess(UUID adminId, UUID branchId) {
        if (!adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)) {
            throw new NotFoundException("Admin doesn't have access to this branch", adminId);
        }
    }
}
