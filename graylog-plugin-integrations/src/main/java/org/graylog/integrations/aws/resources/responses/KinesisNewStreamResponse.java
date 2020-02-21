package org.graylog.integrations.aws.resources.responses;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class KinesisNewStreamResponse {

    private static final String STREAM_NAME = "stream_name";
    private static final String STREAM_ARN = "stream_arn";
    private static final String RESULT = "result";

    @JsonProperty(STREAM_NAME)
    public abstract String streamName();

    @JsonProperty(STREAM_ARN)
    public abstract String streamArn();

    @JsonProperty(RESULT)
    public abstract String result();

    public static KinesisNewStreamResponse create(@JsonProperty(STREAM_NAME) String streamName,
                                                  @JsonProperty(STREAM_ARN) String streamArn,
                                                  @JsonProperty(RESULT) String result) {
        return new AutoValue_KinesisNewStreamResponse(streamName, streamArn, result);
    }
}