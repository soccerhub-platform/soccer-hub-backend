package kz.edu.soccerhub.common.dto.company;
import kz.edu.soccerhub.common.dto.trial.TrialBookingListItemDto;
import lombok.Builder;
import java.util.*;
@Builder
public record TrialCompanyPage(List<TrialBookingListItemDto> content,long totalElements,int totalPages,int number,int size,Map<String,Long> summary) {}
