package org.graylog.aws.sqs;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonDeserialize(builder = S3Bucket.Builder.class)
public abstract class S3Bucket {
    private static final String BUCKET = "bucket";
    private static final String OBJECT = "object";

    @JsonProperty(BUCKET)
    public abstract JsonNode bucket();

    @JsonProperty(OBJECT)
    public abstract JsonNode object();

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static abstract class Builder {

        @JsonProperty(BUCKET)

        public abstract Builder bucket(JsonNode bucket);

        @JsonProperty(OBJECT)
        public abstract Builder object(JsonNode object);

        @JsonCreator
        public static Builder create() {
            return new AutoValue_S3Bucket.Builder();
        }

        public abstract S3Bucket build();

    }
}
