package kz.edu.soccerhub.organization.infrastructure;

import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.GroupMembershipPort;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.common.port.TrialGroupPort;
import kz.edu.soccerhub.organization.domain.model.enums.GroupStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TrialGroupAdapter implements TrialGroupPort {

    private final GroupPort groupPort;
    private final GroupMembershipPort membershipPort;

    @Override
    public void validateAvailableCapacity(
            UUID groupId,
            UUID studentId,
            LocalDate asOfDate
    ) {
        GroupDto group = groupPort.getGroupById(groupId);

        if (group == null) {
            throw new NotFoundException("Group not found", groupId);
        }

        if (group.status() != GroupStatus.ACTIVE) {
            throw new BadRequestException(
                    "Trial can only be booked for an active group",
                    groupId
            );
        }

        if (membershipPort.existsActiveByGroupIdAndPlayerIdAsOfDate(
                groupId,
                studentId,
                asOfDate
        )) {
            throw new BadRequestException(
                    "Student is already enrolled in this group",
                    groupId,
                    studentId
            );
        }

        if (group.capacity() == null) {
            return;
        }

        long activeMembers = membershipPort.countActiveByGroupIdAsOfDate(
                groupId,
                asOfDate
        );

        if (activeMembers >= group.capacity()) {
            throw new BadRequestException(
                    "Group has no available places",
                    groupId
            );
        }
    }

    @Override
    public TrialBookingDetailsDto.Group getDetails(UUID groupId) {
        GroupDto group = groupPort.getGroupById(groupId);

        if (group == null) {
            throw new NotFoundException("Group not found", groupId);
        }

        return TrialBookingDetailsDto.Group.builder()
                .id(group.groupId())
                .name(group.name())
                .build();
    }

    @Override
    public Map<UUID, TrialBookingDetailsDto.Group> getDetails(
            Collection<UUID> groupIds
    ) {
        if (groupIds.isEmpty()) {
            return Map.of();
        }

        return groupPort.getGroupsByIds(Set.copyOf(groupIds)).stream()
                .collect(Collectors.toMap(
                        GroupDto::groupId,
                        group -> TrialBookingDetailsDto.Group.builder()
                                .id(group.groupId())
                                .name(group.name())
                                .build()
                ));
    }
}
