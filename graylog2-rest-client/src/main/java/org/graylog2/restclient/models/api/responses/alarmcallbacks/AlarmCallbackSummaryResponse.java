package org.graylog2.restclient.models.api.responses.alarmcallbacks;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class AlarmCallbackSummaryResponse {
    public String id;
    @JsonProperty("stream_id")
    public String streamId;
    public String type;
    public Map<String, Object> configuration;
    @JsonProperty("created_at")
    public String createdAt;
    @JsonProperty("creator_user_id")
    public String creatorUserId;
}
