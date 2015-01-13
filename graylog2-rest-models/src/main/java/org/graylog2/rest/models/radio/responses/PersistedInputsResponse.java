package org.graylog2.rest.models.radio.responses;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Map;

@AutoValue
public abstract class PersistedInputsResponse {
    @JsonProperty
    public abstract String type();
    @JsonProperty
    public abstract String id();
    @JsonProperty
    public abstract String title();
    @JsonProperty
    public abstract String creatorUserId();
    @JsonProperty
    public abstract String createdAt();
    @JsonProperty
    public abstract Boolean global();
    @JsonProperty
    public abstract Map<String, Object> configuration();

    @JsonCreator
    public static PersistedInputsResponse create(@JsonProperty("type") String type,
                                                 @JsonProperty("id") String id,
                                                 @JsonProperty("title") String title,
                                                 @JsonProperty("creator_user_id") String creatorUserId,
                                                 @JsonProperty("created_at") String createdAt,
                                                 @JsonProperty("global") Boolean global,
                                                 @JsonProperty("configuration") Map<String, Object> configuration) {
        return new AutoValue_PersistedInputsResponse(type, id, title, creatorUserId, createdAt, global, configuration);
    }
}
