package org.graylog2.streams;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog2.database.CollectionName;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.rest.resources.streams.rules.requests.CreateStreamRuleRequest;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static org.graylog2.plugin.streams.StreamRuleType.ALWAYS_MATCH;

@AutoValue
@JsonAutoDetect
@CollectionName("streamrules")
public abstract class StreamRuleImpl implements StreamRule {
    static final String FIELD_ID = "id";
    static final String FIELD_TYPE = "type";
    static final String FIELD_VALUE = "value";
    static final String FIELD_FIELD = "field";
    static final String FIELD_INVERTED = "inverted";
    static final String FIELD_STREAM_ID = "stream_id";
    static final String FIELD_CONTENT_PACK = "content_pack";
    static final String FIELD_DESCRIPTION = "description";

    @Override
    @JsonProperty(FIELD_ID)
    @Id
    @ObjectId
    @Nullable
    public abstract String getId();

    @Override
    @JsonIgnore
    public abstract StreamRuleType getType();

    @JsonProperty(FIELD_TYPE)
    @SuppressWarnings("unused")
    public int getNumericType() {
        return getType().toInteger();
    }

    @Override
    @JsonProperty(FIELD_FIELD)
    public abstract String getField();

    @Override
    @JsonProperty(FIELD_VALUE)
    @Nullable
    public abstract String getValue();

    @Override
    @JsonProperty(FIELD_INVERTED)
    public abstract Boolean getInverted();

    @Override
    @JsonProperty(FIELD_STREAM_ID)
    @ObjectId
    public abstract String getStreamId();

    @Override
    @JsonProperty(FIELD_CONTENT_PACK)
    @Nullable
    public abstract String getContentPack();

    @Override
    @JsonProperty(FIELD_DESCRIPTION)
    @Nullable
    public abstract String getDescription();

    @Override
    public abstract Builder toBuilder();

    static Builder builder() {
        return new AutoValue_StreamRuleImpl.Builder().inverted(false);
    }

    @JsonCreator
    public static StreamRuleImpl create(@JsonProperty(FIELD_ID) @ObjectId @Id String id,
                                        @JsonProperty(FIELD_TYPE) int type,
                                        @JsonProperty(FIELD_FIELD) String field,
                                        @JsonProperty(FIELD_VALUE) @Nullable String value,
                                        @JsonProperty(FIELD_INVERTED) Boolean inverted,
                                        @JsonProperty(FIELD_STREAM_ID) @ObjectId String streamId,
                                        @JsonProperty(FIELD_CONTENT_PACK) @Nullable String contentPack,
                                        @JsonProperty(FIELD_DESCRIPTION) @Nullable String description) {
        final StreamRuleType streamRuleType = StreamRuleType.fromInteger(type);
        checkArgument(streamRuleType != null, "Invalid numeric stream rule type: " + type);
        checkArgument(value != null || streamRuleType.equals(ALWAYS_MATCH), "Value can only be null for " + ALWAYS_MATCH + " rule type.");

        return builder()
            .id(id)
            .type(streamRuleType)
            .field(field)
            .value(value)
            .inverted(inverted)
            .streamId(streamId)
            .contentPack(contentPack)
            .description(description)
            .build();
    }

    public static StreamRule create(String streamId, CreateStreamRuleRequest createStreamRuleRequest) {
        final StreamRuleType streamRuleType = StreamRuleType.fromInteger(createStreamRuleRequest.type());
        checkArgument(streamRuleType != null, "Invalid stream rule type: " + createStreamRuleRequest.type());
        checkArgument(createStreamRuleRequest.value() != null || streamRuleType.equals(ALWAYS_MATCH), "Value can only be null for " + ALWAYS_MATCH + " rule type.");
        return builder()
            .type(streamRuleType)
            .field(createStreamRuleRequest.field())
            .value(createStreamRuleRequest.value())
            .inverted(createStreamRuleRequest.inverted())
            .streamId(streamId)
            .description(createStreamRuleRequest.description())
            .build();
    }

    @AutoValue.Builder
    abstract static class Builder implements StreamRule.Builder {
        abstract Builder id(String id);
        @Override
        public abstract Builder type(StreamRuleType type);
        @Override
        public abstract Builder field(String field);
        @Override
        public abstract Builder value(String value);
        @Override
        public abstract Builder inverted(Boolean inverted);
        @Override
        public abstract Builder streamId(String streamId);
        @Override
        public abstract Builder contentPack(String contentPack);
        @Override
        public abstract Builder description(String description);
        @Override
        public abstract StreamRuleImpl build();
    }
}
