package kz.edu.soccerhub.common.port;
import java.util.*;
import java.time.LocalDate;
import kz.edu.soccerhub.common.dto.trial.TrialSessionContext;
public interface CompanySessionReadPort { java.util.List<kz.edu.soccerhub.common.dto.company.CompanyMetric> attendanceMetrics(Map<UUID,UUID> groupBranches,kz.edu.soccerhub.common.dto.company.CompanyQuery q); Map<UUID,TrialSessionContext> sessions(Set<UUID> groups,LocalDate from,LocalDate to); }
