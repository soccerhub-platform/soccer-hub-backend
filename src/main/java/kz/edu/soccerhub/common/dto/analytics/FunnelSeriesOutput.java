package kz.edu.soccerhub.common.dto.analytics;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import lombok.Getter;

import java.util.Map;

public class FunnelSeriesOutput {

    @Getter
    private final String bucket;
    private final Map<LeadStatus, Long> counts;

    public FunnelSeriesOutput(String bucket, Map<LeadStatus, Long> counts) {
        this.bucket = bucket;
        this.counts = counts;
    }

    @JsonAnyGetter
    public Map<LeadStatus, Long> getCounts() {
        return counts;
    }
}

