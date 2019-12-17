package org.graylog.integrations.aws.resources.requests;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@AutoValue
@WithBeanGetter
@JsonDeserialize(builder = KinesisNewStreamRequest.Builder.class)
public abstract class KinesisNewStreamRequest implements AWSRequest {

    private static final String STREAM_NAME = "stream_name";

    @JsonProperty(STREAM_NAME)
    public abstract String streamName();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public static abstract class Builder implements AWSRequest.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_KinesisNewStreamRequest.Builder();
        }

        @JsonProperty(STREAM_NAME)
        public abstract Builder streamName(String streamName);

        public abstract KinesisNewStreamRequest build();
    }
}