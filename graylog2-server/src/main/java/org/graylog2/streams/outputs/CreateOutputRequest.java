package org.graylog2.streams.outputs;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Set;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@JsonAutoDetect
public class CreateOutputRequest {
    public String title;
    public String type;
    public Map<String, Object> configuration;
    @JsonProperty("creator_user_id")
    public String creatorUserId;
    @JsonProperty("streams")
    public Set<String> streams;
}
