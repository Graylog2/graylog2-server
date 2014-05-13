package org.graylog2.restclient.models.api.responses.alerts;

import com.google.gson.annotations.SerializedName;
import org.joda.time.DateTime;

import java.util.List;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class CheckConditionResponse {
    public List<ConditionTriggeredSummary> results;
    @SerializedName("total_triggered")
    public Integer totalTriggered;
    @SerializedName("calculated_at")
    public String calculatedAt;
}
