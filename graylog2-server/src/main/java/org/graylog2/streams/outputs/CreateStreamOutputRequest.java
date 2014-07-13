package org.graylog2.streams.outputs;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class CreateStreamOutputRequest {
    public String title;
    public String type;
    public Map<String, Object> configuration;
    @JsonProperty("creator_user_id")
    public String creatorUserId;
}
