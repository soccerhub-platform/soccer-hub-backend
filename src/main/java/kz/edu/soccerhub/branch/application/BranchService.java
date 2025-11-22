package kz.edu.soccerhub.branch.application;

import kz.edu.soccerhub.branch.domain.model.BranchEntity;
import kz.edu.soccerhub.branch.domain.repository.BranchRepository;
import kz.edu.soccerhub.common.dto.branch.BranchDto;
import kz.edu.soccerhub.common.dto.branch.CreateBranchCommand;
import kz.edu.soccerhub.common.port.BranchPort;
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
public class BranchService implements BranchPort {

    private final BranchRepository branchRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isExist(UUID branchId) {
        return branchRepository.existsById(branchId);
    }

    @Override
    @Transactional
    public UUID create(CreateBranchCommand command) {
        final UUID branchId = UUID.randomUUID();
        BranchEntity branchEntity = BranchEntity.builder()
                .id(branchId)
                .clubId(command.clubId())
                .name(command.name())
                .address(command.address())
                .active(true)
                .build();

        branchRepository.save(branchEntity);

        return branchId;
    }

    @Override
    public Optional<BranchDto> findById(UUID branchId) {
        return branchRepository.findById(branchId)
                .map(BranchService::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BranchDto> findAllByIds(Collection<UUID> ids) {
        List<BranchEntity> allById = branchRepository.findAllById(ids);
        return allById.stream()
                .map(BranchService::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<BranchDto> findByClubId(UUID clubId) {
        List<BranchEntity> allById = branchRepository.findByClubId(clubId);
        return allById.stream()
                .map(BranchService::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<BranchDto> findByClubIds(Collection<UUID> clubIds) {
        List<BranchEntity> allById = branchRepository.findAllByClubIdIn(clubIds);
        return allById.stream()
                .map(BranchService::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(UUID branchId) {
        branchRepository.deleteById(branchId);
    }

    private static BranchDto toDto(BranchEntity branch) {
        return BranchDto.builder()
                .id(branch.getId())
                .name(branch.getName())
                .address(branch.getAddress())
                .clubId(branch.getClubId())
                .active(branch.isActive())
                .build();
    }
}
