package kz.edu.soccerhub.common.port;
import kz.edu.soccerhub.common.dto.auth.*;
import java.util.UUID;
public interface EmployeeAccessPort {
 EmployeeAccessDto getAccess(UUID userId);
 void changePermissions(ChangeEmployeePermissionsCommand command);
}
