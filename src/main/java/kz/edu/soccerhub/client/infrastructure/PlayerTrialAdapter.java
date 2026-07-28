package kz.edu.soccerhub.client.infrastructure;

import kz.edu.soccerhub.client.domain.model.Player;
import kz.edu.soccerhub.client.domain.repository.PlayerRepository;
import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.TrialStudentDetailsPort;
import kz.edu.soccerhub.common.port.TrialStudentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PlayerTrialAdapter
        implements TrialStudentPort, TrialStudentDetailsPort {

    private final PlayerRepository repository;

    @Override
    public void validateExists(UUID studentId) {
        if (!repository.existsById(studentId)) {
            throw new NotFoundException("Student not found", studentId);
        }
    }

    @Override
    public TrialBookingDetailsDto.Student getDetails(UUID studentId) {
        Player player = repository.findById(studentId)
                .orElseThrow(() -> new NotFoundException(
                        "Student not found",
                        studentId
                ));

        return toStudent(player);
    }

    @Override
    public Map<UUID, TrialBookingDetailsDto.Student> getDetails(
            Collection<UUID> studentIds
    ) {
        if (studentIds.isEmpty()) {
            return Map.of();
        }

        return repository.findAllById(studentIds).stream()
                .collect(Collectors.toMap(
                        Player::getId,
                        this::toStudent
                ));
    }

    private TrialBookingDetailsDto.Student toStudent(Player player) {
        return TrialBookingDetailsDto.Student.builder()
                .id(player.getId())
                .fullName(
                        (player.getFirstName() + " " + player.getLastName()).trim()
                )
                .birthDate(player.getBirthDate())
                .age(calculateAge(player.getBirthDate()))
                .build();
    }

    private int calculateAge(LocalDate birthDate) {
        return Period.between(
                birthDate,
                LocalDate.now()
        ).getYears();
    }
}
