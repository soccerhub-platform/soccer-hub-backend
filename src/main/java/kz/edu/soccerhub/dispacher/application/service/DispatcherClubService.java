package kz.edu.soccerhub.dispacher.application.service;

import kz.edu.soccerhub.dispacher.domain.model.DispatcherClub;
import kz.edu.soccerhub.dispacher.domain.repository.DispatcherClubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DispatcherClubService {

    private final DispatcherClubRepository dispatcherClubRepository;

    @Transactional(readOnly = true)
    public boolean hasAccess(UUID dispatcherId, UUID clubId) {
        DispatcherClub.DispatcherClubId dispatcherClubId = new DispatcherClub.DispatcherClubId();
        dispatcherClubId.setDispatcherId(dispatcherId);
        dispatcherClubId.setClubId(clubId);

        return dispatcherClubRepository.existsById(dispatcherClubId);
    }

    @Transactional
    public void attachDispatcherToClub(UUID dispatcherId, UUID clubId) {
        DispatcherClub.DispatcherClubId dispatcherClubId = new DispatcherClub.DispatcherClubId();
        dispatcherClubId.setDispatcherId(dispatcherId);
        dispatcherClubId.setClubId(clubId);

        DispatcherClub dispatcherClub = DispatcherClub.builder()
                .id(dispatcherClubId)
                .build();

        dispatcherClubRepository.save(dispatcherClub);
    }

    @Transactional(readOnly = true)
    public Collection<UUID> getClubs(UUID dispatcherId) {
        return dispatcherClubRepository.findByIdDispatcherId(dispatcherId)
                .stream()
                .map(DispatcherClub::getId)
                .map(DispatcherClub.DispatcherClubId::getClubId)
                .collect(Collectors.toList());
    }

}
