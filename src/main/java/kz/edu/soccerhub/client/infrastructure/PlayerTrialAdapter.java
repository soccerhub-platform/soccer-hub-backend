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
import java.util.UUID;

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

        return TrialBookingDetailsDto.Student.builder()
                .id(player.getId())
                .fullName(
                        player.getFirstName() + " " + player.getLastName()
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
