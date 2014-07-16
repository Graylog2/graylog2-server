package org.graylog2.rest.resources.streams.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class SingleStreamRuleSummaryResponse {
    @JsonProperty("streamrule_id")
    public String streamRuleId;
}
