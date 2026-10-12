package kz.edu.soccerhub.organization.application.service;

import kz.edu.soccerhub.organization.domain.model.Branch;
import kz.edu.soccerhub.organization.domain.repository.BranchRepository;
import kz.edu.soccerhub.common.dto.branch.BranchDto;
import kz.edu.soccerhub.common.dto.branch.CreateBranchCommand;
import kz.edu.soccerhub.common.dto.branch.UpdateBranchDetailsCommand;
import kz.edu.soccerhub.common.dto.branch.ChangeBranchArchiveCommand;
import kz.edu.soccerhub.common.dto.branch.BranchWorkingDay;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import java.time.Instant;
import java.time.DayOfWeek;
import java.util.HashSet;
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
    private final BranchHistoryService history;

    @Override
    @Transactional(readOnly = true)
    public boolean isExist(UUID branchId) {
        return branchRepository.existsById(branchId);
    }

    @Override
    @Transactional
    public UUID create(CreateBranchCommand command) {
        validateDetails(command.name(), command.address(), command.phone(), command.email(), command.workingHours());
        final UUID branchId = UUID.randomUUID();
        Branch branch = Branch.builder()
                .id(branchId)
                .clubId(command.clubId())
                .name(command.name().trim())
                .address(optional(command.address()))
                .phone(optional(command.phone()))
                .email(optional(command.email()))
                .managerAdminId(command.managerAdminId())
                .workingHours(normalizeHours(command.workingHours()))
                .active(true)
                .build();

        branchRepository.saveAndFlush(branch);
        history.record("CREATED",null,toDto(branch));

        return branchId;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BranchDto> findById(UUID branchId) {
        return branchRepository.findById(branchId)
                .map(BranchService::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<BranchDto> findAllByIds(Collection<UUID> ids) {
        List<Branch> allById = branchRepository.findAllById(ids);
        return allById.stream()
                .map(BranchService::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<BranchDto> findByClubId(UUID clubId) {
        List<Branch> allById = branchRepository.findByClubId(clubId);
        return allById.stream()
                .map(BranchService::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<BranchDto> findByClubIds(Collection<UUID> clubIds) {
        List<Branch> allById = branchRepository.findAllByClubIdIn(clubIds);
        return allById.stream()
                .map(BranchService::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(UUID branchId) {
        branchRepository.deleteById(branchId);
    }

    @Override
    @Transactional
    public void updateDetails(UpdateBranchDetailsCommand command) {
        validateDetails(command.name(), command.address(), command.phone(), command.email(), command.workingHours());
        Branch branch = branchRepository.findForUpdate(command.branchId())
                .orElseThrow(() -> new NotFoundException("Branch not found", command.branchId()));
        if (!branch.isActive()) {
            throw new BadRequestException("Сначала восстановите филиал из архива");
        }
        var before=toDto(branch);
        branch.setName(command.name().trim());
        branch.setAddress(optional(command.address()));
        branch.setPhone(optional(command.phone()));
        branch.setEmail(optional(command.email()));
        branch.setManagerAdminId(command.managerAdminId());
        branch.setWorkingHours(normalizeHours(command.workingHours()));
        branchRepository.save(branch);
        history.record("UPDATED",before,toDto(branch));
    }

    @Override
    @Transactional
    public void changeArchive(ChangeBranchArchiveCommand command) {
        Branch branch = branchRepository.findForUpdate(command.branchId())
                .orElseThrow(() -> new NotFoundException("Branch not found", command.branchId()));
        if (branch.isActive() == !command.archived()) return;
        var before=toDto(branch);
        branch.setActive(!command.archived());
        branch.setArchivedAt(command.archived() ? Instant.now() : null);
        branchRepository.save(branch);
        history.record(command.archived()?"ARCHIVED":"RESTORED",before,toDto(branch));
    }

    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static List<BranchWorkingDay> normalizeHours(List<BranchWorkingDay> hours) {
        if (hours == null || hours.isEmpty()) return List.of();
        return hours.stream().sorted(java.util.Comparator.comparing(BranchWorkingDay::day))
                .map(day -> day.closed()
                        ? new BranchWorkingDay(day.day(), true, null, null) : day)
                .toList();
    }

    private static void validateDetails(String name, String address, String phone, String email,
                                        List<BranchWorkingDay> hours) {
        if (name == null || name.isBlank() || name.trim().length() > 255) {
            throw new BadRequestException("Укажите название филиала до 255 символов");
        }
        if (address != null && address.length() > 500) {
            throw new BadRequestException("Адрес не должен превышать 500 символов");
        }
        if (optional(phone) != null && !phone.trim().matches("\\+[1-9][0-9]{6,14}")) {
            throw new BadRequestException("Укажите телефон в международном формате");
        }
        if (optional(email) != null && (email.trim().length() > 254 || !email.trim().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))) {
            throw new BadRequestException("Укажите корректный email филиала");
        }
        if (hours == null || hours.isEmpty()) return;
        if (hours.size() != 7) throw new BadRequestException("График должен содержать все семь дней недели");
        var days = new HashSet<DayOfWeek>();
        for (var day : hours) {
            if (day == null || day.day() == null || !days.add(day.day())) {
                throw new BadRequestException("Дни недели в графике не должны повторяться");
            }
            if (!day.closed() && (day.opensAt() == null || day.closesAt() == null
                    || !day.closesAt().isAfter(day.opensAt())
                    || day.opensAt().getSecond() != 0 || day.closesAt().getSecond() != 0
                    || day.opensAt().getNano() != 0 || day.closesAt().getNano() != 0)) {
                throw new BadRequestException("Укажите время открытия и закрытия; закрытие должно быть позже открытия");
            }
        }
    }

    private static BranchDto toDto(Branch branch) {
        return BranchDto.builder()
                .id(branch.getId())
                .name(branch.getName())
                .address(branch.getAddress())
                .clubId(branch.getClubId())
                .timezone(branch.getClub() == null ? null : branch.getClub().getTimezone())
                .active(branch.isActive())
                .phone(branch.getPhone())
                .email(branch.getEmail())
                .managerAdminId(branch.getManagerAdminId())
                .workingHours(branch.getWorkingHours())
                .archivedAt(branch.getArchivedAt())
                .build();
    }
}
