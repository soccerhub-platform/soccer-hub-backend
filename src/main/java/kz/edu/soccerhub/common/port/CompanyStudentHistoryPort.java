package kz.edu.soccerhub.common.port;
import kz.edu.soccerhub.common.dto.company.*;
import java.util.*;
public interface CompanyStudentHistoryPort { CompanyEntry student(UUID id,Set<UUID> branches); }
