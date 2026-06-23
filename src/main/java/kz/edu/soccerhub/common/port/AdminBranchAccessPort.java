package kz.edu.soccerhub.common.port;

import java.util.UUID;

public interface AdminBranchAccessPort {
    boolean verifyAdminBelongsToBranch(UUID adminId, UUID branchId);
}
