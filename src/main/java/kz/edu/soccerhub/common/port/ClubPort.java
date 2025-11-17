package kz.edu.soccerhub.common.port;

import jakarta.validation.constraints.NotNull;
import kz.edu.soccerhub.common.dto.club.ClubDto;
import kz.edu.soccerhub.common.dto.club.CreateClubCommand;

import java.util.Collection;
import java.util.UUID;

public interface ClubPort {
    UUID create(CreateClubCommand command);
    Collection<ClubDto> findAllByIds(Collection<UUID> ids);
    boolean isExist(@NotNull UUID uuid);

}