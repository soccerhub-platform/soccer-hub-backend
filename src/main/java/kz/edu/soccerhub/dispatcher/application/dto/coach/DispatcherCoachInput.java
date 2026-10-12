package kz.edu.soccerhub.dispatcher.application.dto.coach;
import jakarta.validation.constraints.*;
import lombok.Builder;
import java.time.LocalDate;
import java.util.UUID;
@Builder
public record DispatcherCoachInput(@NotBlank @Size(max=100) String firstName,@NotBlank @Size(max=100) String lastName,
 @NotBlank @Email @Size(max=100) String email,@NotNull @Past LocalDate birthDate,
 @NotBlank @Pattern(regexp="\\+[1-9][0-9]{6,14}") String phone,@Size(max=200) String specialization,@Size(max=5000) String bio,UUID branchId) {}
