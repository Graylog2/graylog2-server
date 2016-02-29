package org.graylog2.rest.resources.streams.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.rest.models.system.outputs.responses.OutputSummary;

import javax.annotation.Nullable;
import java.util.Collection;

@AutoValue
@JsonAutoDetect
public abstract class StreamResponse {
    @JsonProperty("creator_user_id")
    public abstract String creatorUserId();

    @JsonProperty("outputs")
    public abstract Collection<OutputSummary> outputs();

    @JsonProperty("matching_type")
    public abstract String matchingType();

    @JsonProperty("description")
    public abstract String description();

    @JsonProperty("created_at")
    public abstract String createdAt();

    @JsonProperty("disabled")
    public abstract boolean disabled();

    @JsonProperty("rules")
    public abstract Collection<StreamRule> rules();

    @JsonProperty("title")
    public abstract String title();

    @JsonProperty("content_pack")
    @Nullable
    public abstract String contentPack();

    @JsonCreator
    public static StreamResponse create(@JsonProperty("creator_user_id") String creatorUserId,
                                        @JsonProperty("outputs") Collection<OutputSummary> outputs,
                                        @JsonProperty("matching_type") String matchingType,
                                        @JsonProperty("description") String description,
                                        @JsonProperty("created_at") String createdAt,
                                        @JsonProperty("disabled") boolean disabled,
                                        @JsonProperty("rules") Collection<StreamRule> rules,
                                        @JsonProperty("title") String title,
                                        @JsonProperty("content_pack") @Nullable String contentPack) {
        return new AutoValue_StreamResponse(creatorUserId, outputs, matchingType, description, createdAt, disabled, rules, title, contentPack);
    }
}
