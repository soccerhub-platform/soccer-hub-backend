package kz.edu.soccerhub.dispatcher.application.service;

import kz.edu.soccerhub.common.dto.branch.BranchDto;
import kz.edu.soccerhub.common.dto.profile.BranchRef;
import kz.edu.soccerhub.common.dto.profile.DispatcherProfileOutput;
import kz.edu.soccerhub.common.dto.profile.ProfileUpdateInput;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.AuthPort;
import kz.edu.soccerhub.common.port.BranchPort;
import kz.edu.soccerhub.dispatcher.domain.model.DispatcherProfile;
import kz.edu.soccerhub.dispatcher.domain.repository.DispatcherProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DispatcherProfileService {

    private final DispatcherProfileRepository dispatcherProfileRepository;
    private final DispatcherClubService dispatcherClubService;
    private final BranchPort branchPort;
    private final AuthPort authPort;

    @Transactional(readOnly = true)
    public DispatcherProfileOutput getProfile(UUID dispatcherId) {
        DispatcherProfile profile = getDispatcher(dispatcherId);
        return toOutput(profile);
    }

    @Transactional
    public DispatcherProfileOutput updateProfile(UUID dispatcherId, ProfileUpdateInput input) {
        DispatcherProfile profile = getDispatcher(dispatcherId);

        profile.setFirstName(input.firstName().trim());
        profile.setLastName(input.lastName().trim());
        profile.setEmail(input.email().trim().toLowerCase());
        profile.setPhone(input.phone().trim());
        profile.setSpecialization(trimToNull(input.specialization()));
        authPort.updateEmail(dispatcherId, profile.getEmail());

        return toOutput(profile);
    }

    private DispatcherProfile getDispatcher(UUID dispatcherId) {
        return dispatcherProfileRepository.findById(dispatcherId)
                .orElseThrow(() -> new NotFoundException("Dispatcher not found", dispatcherId));
    }

    private DispatcherProfileOutput toOutput(DispatcherProfile profile) {
        List<BranchRef> branches = branchPort.findByClubIds(dispatcherClubService.getAll(profile.getId())).stream()
                .map(this::toBranchRef)
                .toList();

        return new DispatcherProfileOutput(
                profile.getId(),
                profile.getFirstName(),
                profile.getLastName(),
                profile.getEmail() == null ? authPort.getEmail(profile.getId()) : profile.getEmail(),
                profile.getPhone(),
                profile.getSpecialization(),
                Boolean.FALSE.equals(profile.getActive()) ? "INACTIVE" : "ACTIVE",
                branches,
                List.of("CLUBS", "BRANCHES", "ADMINS", "ANALYTICS"),
                profile.getCreatedAt()
        );
    }

    private BranchRef toBranchRef(BranchDto branch) {
        return new BranchRef(branch.id(), branch.name());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
