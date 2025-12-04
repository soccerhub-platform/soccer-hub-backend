package kz.edu.soccerhub.club.application;

import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.club.domain.model.ClubEntity;
import kz.edu.soccerhub.club.domain.repository.ClubRepository;
import kz.edu.soccerhub.common.dto.club.ClubDto;
import kz.edu.soccerhub.common.dto.club.CreateClubCommand;
import kz.edu.soccerhub.common.port.ClubPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClubService implements ClubPort {

    private final ClubRepository clubRepository;

    @Override
    public boolean isExist(UUID clubId) {
        return clubRepository.existsById(clubId);
    }

    @Override
    @Transactional
    public UUID create(CreateClubCommand command) {

        final UUID clubId = UUID.randomUUID();
        final String timezone = ZoneId.systemDefault().getId();

        ClubEntity clubEntity = ClubEntity.builder()
                .id(clubId)
                .name(command.name())
                .slug(command.slug())
                .email(command.email())
                .phone(command.phone())
                .website(command.website())
                .address(command.address())
                .timezone(timezone)
                .isActive(true)
                .build();

        clubRepository.save(clubEntity);

        return clubId;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ClubDto> findAllByIds(Collection<UUID> ids) {
        List<ClubEntity> allById = clubRepository.findAllById(ids);
        return allById.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<ClubDto> findById(@NotNull UUID id) {
        return clubRepository.findById(id)
                .map(this::toDto);
    }

    @Override
    @Transactional
    public void delete(UUID clubId) {
        clubRepository.deleteById(clubId);
    }


    private ClubDto toDto(ClubEntity club) {
        return ClubDto.builder()
                .id(club.getId())
                .name(club.getName())
                .slug(club.getSlug())
                .phoneNumber(club.getPhone())
                .email(club.getEmail())
                .address(club.getAddress())
                .active(club.isActive())
                .logoUrl(club.getLogoUrl())
                .website(club.getWebsite())
                .build();
    }
}
