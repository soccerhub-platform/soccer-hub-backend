package kz.edu.soccerhub.common.port;
import kz.edu.soccerhub.common.dto.branch.BranchHistoryOutput;
import org.springframework.data.domain.Page;
import java.util.UUID;
public interface BranchHistoryPort { Page<BranchHistoryOutput> history(UUID branchId,int page,int size); }
