package kz.edu.soccerhub.common.port;
import kz.edu.soccerhub.common.dto.company.*;
import java.util.*;
public interface TrialWorkflowPort {void update(UUID id,UUID actor,TrialMutationCommand c);List<TimelineEvent> history(UUID id);}
