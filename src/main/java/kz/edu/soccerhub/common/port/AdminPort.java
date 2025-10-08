package kz.edu.soccerhub.common.port;

import java.util.UUID;

public interface AdminPort {

    UUID create(UUID userId,
                String firstName,
                String lastName,
                String phone,
                UUID branchId);

}
