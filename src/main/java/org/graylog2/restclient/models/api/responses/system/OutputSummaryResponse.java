package org.graylog2.restclient.models.api.responses.system;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class OutputSummaryResponse {
    public String title;
    public String _id;
    @JsonProperty("creator_user_id")
    public String creatorUserId;
    @JsonProperty("created_at")
    public String createdAt;
    public Map<String, Object> configuration;
    public String type;
}
