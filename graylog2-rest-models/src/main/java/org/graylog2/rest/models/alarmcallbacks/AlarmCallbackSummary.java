package org.graylog2.rest.models.alarmcallbacks;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;

import java.util.Map;

@AutoValue
@JsonAutoDetect
public abstract class AlarmCallbackSummary {
    @JsonProperty
    public abstract String id();
    @JsonProperty("stream_id")
    public abstract String streamId();
    @JsonProperty
    public abstract String type();
    @JsonProperty
    public abstract Map<String, Object> configuration();
    @JsonProperty("created_at")
    public abstract DateTime createdAt();
    @JsonProperty("creator_user_id")
    public abstract String creatorUserId();

    @JsonCreator
    public static AlarmCallbackSummary create(@JsonProperty("id") String id,
                                              @JsonProperty("stream_id") String streamId,
                                              @JsonProperty("type") String type,
                                              @JsonProperty("configuration") Map<String, Object> configuration,
                                              @JsonProperty("created_at") DateTime createdAt,
                                              @JsonProperty("creator_user_id") String creatorUserId) {
        return new AutoValue_AlarmCallbackSummary(id, streamId, type, configuration, createdAt, creatorUserId);
    }
}
