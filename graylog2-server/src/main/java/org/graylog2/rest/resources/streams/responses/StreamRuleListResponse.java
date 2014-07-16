package org.graylog2.rest.resources.streams.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.plugin.streams.StreamRule;

import java.util.Collection;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class StreamRuleListResponse {
    public long total;

    @JsonProperty(value = "stream_rules")
    public Collection<StreamRule> streamRules;
}
