package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.admin.AdminCreateCommand;
import kz.edu.soccerhub.common.dto.admin.AdminCreateCommandOutput;

public interface AdminPort {

    AdminCreateCommandOutput create(AdminCreateCommand command);

}
