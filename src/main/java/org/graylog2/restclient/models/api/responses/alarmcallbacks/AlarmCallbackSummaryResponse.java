package org.graylog2.restclient.models.api.responses.alarmcallbacks;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class AlarmCallbackSummaryResponse {
    public String id;
    @SerializedName("stream_id")
    public String streamId;
    public String type;
    public Map<String, Object> configuration;
    @SerializedName("created_at")
    public String createdAt;
    @SerializedName("creator_user_id")
    public String creatorUserId;
}
