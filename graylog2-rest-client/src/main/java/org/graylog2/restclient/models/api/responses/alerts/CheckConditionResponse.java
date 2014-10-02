package org.graylog2.restclient.models.api.responses.alerts;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

import java.util.List;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class CheckConditionResponse {
    public List<ConditionTriggeredSummary> results;
    @JsonProperty("total_triggered")
    public Integer totalTriggered;
    @JsonProperty("calculated_at")
    public String calculatedAt;
}
