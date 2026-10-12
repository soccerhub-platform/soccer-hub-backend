package kz.edu.soccerhub.common.dto.company;
import lombok.Builder;
import kz.edu.soccerhub.common.dto.contract.ContractDetailsOutput;
import kz.edu.soccerhub.common.dto.lead.LeadOutput;
import kz.edu.soccerhub.common.dto.trial.TrialBookingListItemDto;
import java.util.*;
@Builder
public record StudentHistory(CompanyEntry student,List<ContractDetailsOutput> contracts,List<CompanyEntry> payments,List<LeadOutput> leads,List<TimelineEvent> events,List<TrialBookingListItemDto> trials) {}
