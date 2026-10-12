package kz.edu.soccerhub.common.dto.company;
import lombok.Builder;
import java.time.LocalDate;
@Builder
public record CompanyParticipant(String fullName, LocalDate birthDate) {}
