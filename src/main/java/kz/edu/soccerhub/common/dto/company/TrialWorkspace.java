package kz.edu.soccerhub.common.dto.company;
import kz.edu.soccerhub.common.dto.trial.TrialBookingDetailsDto;
import lombok.Builder;
import java.util.List;
@Builder
public record TrialWorkspace(TrialBookingDetailsDto details,long version,List<TimelineEvent> history) {}
