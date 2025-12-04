package kz.edu.soccerhub.dispacher.application.service;

import kz.edu.soccerhub.common.dto.club.ClubDto;
import kz.edu.soccerhub.common.dto.club.CreateClubCommand;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.port.ClubPort;
import kz.edu.soccerhub.dispacher.application.dto.club.DispatcherClubCreateInput;
import kz.edu.soccerhub.dispacher.application.dto.club.DispatcherClubCreateOutput;
import kz.edu.soccerhub.dispacher.application.dto.club.DispatcherClubsOutput;
import kz.edu.soccerhub.dispacher.domain.model.DispatcherClub;
import kz.edu.soccerhub.dispacher.domain.repository.DispatcherClubRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DispatcherClubService {

    private final DispatcherClubRepository dispatcherClubRepository;
    private final ClubPort clubPort;

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
    public Collection<UUID> getAll(UUID dispatcherId) {
        return dispatcherClubRepository.findByIdDispatcherId(dispatcherId)
                .stream()
                .map(DispatcherClub::getId)
                .map(DispatcherClub.DispatcherClubId::getClubId)
                .collect(Collectors.toList());
    }

    @Transactional
    public DispatcherClubCreateOutput createClub(UUID dispatcherId, DispatcherClubCreateInput input) {

        UUID clubId = clubPort.create(
                CreateClubCommand.builder()
                        .name(input.name())
                        .slug(input.slug())
                        .email(input.email())
                        .phone(input.phone())
                        .address(input.address())
                        .build()
        );

        this.attachDispatcherToClub(dispatcherId, clubId);

        return DispatcherClubCreateOutput.builder()
                .clubId(clubId)
                .build();
    }

    @Transactional(readOnly = true)
    public List<DispatcherClubsOutput> getDispatcherClubs(UUID dispatcherId) {
        Collection<UUID> clubIds = this.getAll(dispatcherId);
        Collection<ClubDto> clubs = clubPort.findAllByIds(clubIds);

        return clubs.stream()
                .map(club ->
                        DispatcherClubsOutput.builder()
                                .clubId(club.id())
                                .name(club.name())
                                .slug(club.slug())
                                .phoneNumber(club.phoneNumber())
                                .address(club.address())
                        .build())
                .toList();
    }

    @Transactional
    public void deleteClub(UUID dispatcherId, UUID clubId) {
        Optional<UUID> dispatcherClub = this.getAll(dispatcherId).stream()
                .filter(id -> id.equals(clubId))
                .findFirst();

        if (dispatcherClub.isEmpty()) {
            throw new BadRequestException("Dispatcher does not have access to club", clubId);
        }

        clubPort.delete(clubId);
    }

}
