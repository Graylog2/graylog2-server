package org.graylog.integrations.aws.resources.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@AutoValue
@WithBeanGetter
@JsonDeserialize(builder = KinesisFullSetupRequest.Builder.class)
public abstract class KinesisFullSetupRequest implements AWSRequest {

    private static final String LOG_GROUP_NAME = "log_group_name";
    private static final String STREAM_NAME = "stream_name";
    private static final String ROLE_POLICY_NAME = "role_policy_name";
    private static final String FILTER_NAME = "filter_name";
    private static final String FILTER_PATTERN = "filter_pattern";

    @JsonProperty(LOG_GROUP_NAME)
    public abstract String getLogGroupName();

    @JsonProperty(STREAM_NAME)
    public abstract String streamName();

    @JsonProperty(ROLE_POLICY_NAME)
    public abstract String rolePolicyName();

    @JsonProperty(FILTER_NAME)
    public abstract String filterName();

    @JsonProperty(FILTER_PATTERN)
    public abstract String filterPattern();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public static abstract class Builder implements AWSRequest.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_KinesisFullSetupRequest.Builder();
        }

        @JsonProperty(LOG_GROUP_NAME)
        public abstract Builder getLogGroupName(String getLogGroupName);

        @JsonProperty(STREAM_NAME)
        public abstract Builder streamName(String streamName);

        @JsonProperty(ROLE_POLICY_NAME)
        public abstract Builder rolePolicyName(String rolePolicyName);

        @JsonProperty(FILTER_NAME)
        public abstract Builder filterName(String filterName);

        @JsonProperty(FILTER_PATTERN)
        public abstract Builder filterPattern(String filterPattern);

        public abstract KinesisFullSetupRequest build();
    }
}