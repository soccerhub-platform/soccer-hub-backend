package kz.edu.soccerhub.common.port;
import kz.edu.soccerhub.common.dto.company.StudentHistory;
import java.util.*;
public interface StudentJourneyPort {StudentHistory read(UUID actor,Set<UUID> branches,UUID student);}
