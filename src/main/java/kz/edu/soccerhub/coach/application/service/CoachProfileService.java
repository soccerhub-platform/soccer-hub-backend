package kz.edu.soccerhub.coach.application.service;

import kz.edu.soccerhub.auth.domain.repository.AppUserRepo;
import kz.edu.soccerhub.coach.application.dto.profile.*;
import kz.edu.soccerhub.coach.domain.model.CoachAvailability;
import kz.edu.soccerhub.coach.domain.model.CoachBranch;
import kz.edu.soccerhub.coach.domain.model.CoachNotificationSettings;
import kz.edu.soccerhub.coach.domain.model.CoachProfile;
import kz.edu.soccerhub.coach.domain.repository.CoachAvailabilityRepository;
import kz.edu.soccerhub.coach.domain.repository.CoachBranchRepository;
import kz.edu.soccerhub.coach.domain.repository.CoachNotificationSettingsRepository;
import kz.edu.soccerhub.coach.domain.repository.CoachProfileRepository;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.ForbiddenException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.organization.domain.model.Branch;
import kz.edu.soccerhub.organization.domain.model.Group;
import kz.edu.soccerhub.organization.domain.model.GroupCoach;
import kz.edu.soccerhub.organization.domain.repository.BranchRepository;
import kz.edu.soccerhub.organization.domain.repository.GroupCoachRepository;
import kz.edu.soccerhub.organization.domain.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CoachProfileService {

    private static final String DEFAULT_TIMEZONE = "Asia/Almaty";
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final Set<String> ALLOWED_DAYS = Set.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");

    private final CoachProfileRepository coachProfileRepository;
    private final CoachBranchRepository coachBranchRepository;
    private final BranchRepository branchRepository;
    private final GroupCoachRepository groupCoachRepository;
    private final GroupRepository groupRepository;
    private final CoachAvailabilityRepository coachAvailabilityRepository;
    private final CoachNotificationSettingsRepository coachNotificationSettingsRepository;
    private final AppUserRepo appUserRepo;

    @Transactional(readOnly = true)
    public CoachProfileResponse getProfile(UUID currentUserId) {
        CoachProfile profile = getCoachProfile(currentUserId);
        return buildProfileResponse(profile);
    }

    @Transactional
    public CoachProfileResponse updateProfile(UUID currentUserId, CoachProfileUpdateRequest request) {
        CoachProfile profile = getCoachProfile(currentUserId);

        profile.setFirstName(request.firstName().trim());
        profile.setLastName(request.lastName().trim());
        profile.setPhone(request.phone().trim());
        profile.setEmail(request.email().trim().toLowerCase());
        profile.setSpecialization(trimToNull(request.specialization()));
        profile.setBio(trimToNull(request.bio()));

        appUserRepo.findById(currentUserId).ifPresent(user -> user.setEmail(profile.getEmail()));

        return buildProfileResponse(profile);
    }

    @Transactional(readOnly = true)
    public CoachAvailabilityResponse getAvailability(UUID currentUserId) {
        getCoachProfile(currentUserId);
        CoachAvailability availability = coachAvailabilityRepository.findById(currentUserId)
                .orElseGet(() -> CoachAvailability.builder()
                        .coachId(currentUserId)
                        .days("MON,TUE,WED,THU,FRI")
                        .timeFrom(LocalTime.of(10, 0))
                        .timeTo(LocalTime.of(20, 0))
                        .timezone(DEFAULT_TIMEZONE)
                        .build());
        return toAvailabilityResponse(availability);
    }

    @Transactional
    public CoachAvailabilityResponse updateAvailability(UUID currentUserId, CoachAvailabilityUpdateRequest request) {
        getCoachProfile(currentUserId);
        validateAvailabilityRequest(request);

        List<String> normalizedDays = request.days().stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .toList();

        LocalTime timeFrom = LocalTime.parse(request.timeFrom().trim(), TIME_FORMAT);
        LocalTime timeTo = LocalTime.parse(request.timeTo().trim(), TIME_FORMAT);

        CoachAvailability availability = coachAvailabilityRepository.findById(currentUserId)
                .orElseGet(() -> CoachAvailability.builder()
                        .coachId(currentUserId)
                        .build());

        availability.setDays(String.join(",", normalizedDays));
        availability.setTimeFrom(timeFrom);
        availability.setTimeTo(timeTo);
        availability.setTimezone(request.timezone().trim());

        coachAvailabilityRepository.save(availability);
        return toAvailabilityResponse(availability);
    }

    @Transactional(readOnly = true)
    public CoachNotificationSettingsResponse getNotificationSettings(UUID currentUserId) {
        getCoachProfile(currentUserId);
        CoachNotificationSettings settings = coachNotificationSettingsRepository.findById(currentUserId)
                .orElseGet(() -> CoachNotificationSettings.builder().coachId(currentUserId).build());
        return new CoachNotificationSettingsResponse(
                settings.isTodaySessions(),
                settings.isOverdueReports(),
                settings.isScheduleChanges()
        );
    }

    @Transactional
    public CoachNotificationSettingsResponse updateNotificationSettings(
            UUID currentUserId,
            CoachNotificationSettingsUpdateRequest request
    ) {
        getCoachProfile(currentUserId);
        CoachNotificationSettings settings = coachNotificationSettingsRepository.findById(currentUserId)
                .orElseGet(() -> CoachNotificationSettings.builder().coachId(currentUserId).build());

        settings.setTodaySessions(request.todaySessions());
        settings.setOverdueReports(request.overdueReports());
        settings.setScheduleChanges(request.scheduleChanges());
        coachNotificationSettingsRepository.save(settings);

        return new CoachNotificationSettingsResponse(
                settings.isTodaySessions(),
                settings.isOverdueReports(),
                settings.isScheduleChanges()
        );
    }

    private CoachProfileResponse buildProfileResponse(CoachProfile profile) {
        List<CoachBranch> coachBranches = coachBranchRepository.findAllByCoachId(profile.getId());
        Map<UUID, Branch> branchesById = branchRepository.findAllById(
                        coachBranches.stream().map(CoachBranch::getBranchId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Branch::getId, b -> b));

        List<CoachBranchItem> branches = coachBranches.stream()
                .map(cb -> {
                    Branch branch = branchesById.get(cb.getBranchId());
                    return new CoachBranchItem(
                            cb.getBranchId(),
                            branch == null ? "Unknown branch" : branch.getName()
                    );
                })
                .toList();

        List<GroupCoach> groupCoaches = groupCoachRepository.findByCoachIdAndActiveTrue(profile.getId());
        Map<UUID, Group> groupsById = groupRepository.findAllById(
                        groupCoaches.stream().map(GroupCoach::getGroupId).collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Group::getId, g -> g));

        List<CoachGroupItem> groups = groupCoaches.stream()
                .map(gc -> {
                    Group group = groupsById.get(gc.getGroupId());
                    UUID branchId = group == null ? null : group.getBranchId();
                    Branch branch = branchId == null ? null : branchesById.get(branchId);
                    return new CoachGroupItem(
                            gc.getGroupId(),
                            group == null ? "Unknown group" : group.getName(),
                            branchId,
                            branch == null ? "Unknown branch" : branch.getName(),
                            gc.getRole() == null ? null : gc.getRole().name()
                    );
                })
                .toList();

        return new CoachProfileResponse(
                profile.getId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getEmail(),
                profile.getPhone(),
                profile.getSpecialization(),
                profile.getBio(),
                profile.getStatus() == null ? null : profile.getStatus().name(),
                branches,
                groups,
                profile.getCreatedAt()
        );
    }

    private CoachProfile getCoachProfile(UUID currentUserId) {
        CoachProfile profile = coachProfileRepository.findById(currentUserId)
                .orElseThrow(() -> new ForbiddenException("Coach profile not found"));
        if (profile.getStatus() == null) {
            throw new NotFoundException("Coach status not found", currentUserId);
        }
        return profile;
    }

    private CoachAvailabilityResponse toAvailabilityResponse(CoachAvailability availability) {
        List<String> days = Arrays.stream(availability.getDays().split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
        return new CoachAvailabilityResponse(
                days,
                availability.getTimeFrom().format(TIME_FORMAT),
                availability.getTimeTo().format(TIME_FORMAT),
                availability.getTimezone()
        );
    }

    private void validateAvailabilityRequest(CoachAvailabilityUpdateRequest request) {
        List<String> normalizedDays = request.days().stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .toList();
        if (normalizedDays.isEmpty()) {
            throw new BadRequestException("days is required");
        }
        boolean hasInvalidDay = normalizedDays.stream().anyMatch(day -> !ALLOWED_DAYS.contains(day));
        if (hasInvalidDay) {
            throw new BadRequestException("days contains invalid value", request.days());
        }

        LocalTime timeFrom;
        LocalTime timeTo;
        try {
            timeFrom = LocalTime.parse(request.timeFrom().trim(), TIME_FORMAT);
            timeTo = LocalTime.parse(request.timeTo().trim(), TIME_FORMAT);
        } catch (Exception ex) {
            throw new BadRequestException("Invalid time format. Expected HH:mm");
        }
        if (!timeFrom.isBefore(timeTo)) {
            throw new BadRequestException("timeFrom must be before timeTo", request.timeFrom(), request.timeTo());
        }

        try {
            ZoneId.of(request.timezone().trim());
        } catch (Exception ex) {
            throw new BadRequestException("Invalid timezone", request.timezone());
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
