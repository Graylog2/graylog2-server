package org.graylog2.streams.filters;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilder;
import org.graylog2.database.DbEntity;
import org.graylog2.database.MongoEntity;
import org.graylog2.shared.security.RestPermissions;

import java.util.Optional;

@AutoValue
@WithBeanGetter
@JsonAutoDetect
@JsonDeserialize(builder = StreamOutputFilterRuleDTO.Builder.class)
@DbEntity(collection = StreamOutputFilterService.COLLECTION, readPermission = RestPermissions.STREAM_OUTPUT_FILTERS_READ)
public abstract class StreamOutputFilterRuleDTO implements MongoEntity {
    public enum Status {
        @JsonProperty("enabled")
        ENABLED,
        @JsonProperty("disabled")
        DISABLED;
    }

    private static final String FIELD_TITLE = "title";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_STREAM_ID = "stream_id";
    private static final String FIELD_OUTPUT_TARGET = "output_target";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_RULE = "rule";

    @JsonProperty(FIELD_TITLE)
    @NotBlank
    public abstract String title();

    @JsonProperty(FIELD_DESCRIPTION)
    public abstract Optional<String> description();

    @JsonProperty(FIELD_STREAM_ID)
    @NotBlank
    public abstract String streamId();

    @JsonProperty(FIELD_OUTPUT_TARGET)
    @NotBlank
    public abstract String outputTarget();

    @JsonProperty(FIELD_STATUS)
    @NotBlank
    public abstract Status status();

    @JsonProperty(FIELD_STATUS)
    public abstract RuleBuilder rule();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_StreamOutputFilterRuleDTO.Builder()
                    .status(Status.DISABLED);
        }

        @JsonProperty(FIELD_TITLE)
        public abstract Builder title(String title);

        @JsonProperty(FIELD_STREAM_ID)
        public abstract Builder description(@Nullable String description);

        @JsonProperty(FIELD_STREAM_ID)
        public abstract Builder streamId(String streamId);

        @JsonProperty(FIELD_OUTPUT_TARGET)
        public abstract Builder outputTarget(String outputTarget);

        @JsonProperty(FIELD_STATUS)
        public abstract Builder status(Status status);

        @JsonProperty(FIELD_RULE)
        public abstract Builder rule(RuleBuilder rule);

        public abstract StreamOutputFilterRuleDTO build();
    }
}
