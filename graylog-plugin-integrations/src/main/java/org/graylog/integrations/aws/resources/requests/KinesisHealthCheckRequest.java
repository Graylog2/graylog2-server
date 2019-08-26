package org.graylog.integrations.aws.resources.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class KinesisHealthCheckRequest implements AWSRequest {

    private static final String STREAM_NAME = "stream_name";
    private static final String LOG_GROUP_NAME = "log_group_name";

    @JsonProperty(REGION)
    public abstract String region();

    @JsonProperty(AWS_ACCESS_KEY_ID)
    public abstract String awsAccessKeyId();

    @JsonProperty(AWS_SECRET_ACCESS_KEY)
    public abstract String awsSecretAccessKey();

    @JsonProperty(STREAM_NAME)
    public abstract String streamName();

    @JsonCreator
    public static KinesisHealthCheckRequest create(@JsonProperty(REGION) String region,
                                                   @JsonProperty(AWS_ACCESS_KEY_ID) String awsAccessKeyId,
                                                   @JsonProperty(AWS_SECRET_ACCESS_KEY) String awsSecretAccessKey,
                                                   @JsonProperty(STREAM_NAME) String streamName) {
        return new AutoValue_KinesisHealthCheckRequest(region, awsAccessKeyId, awsSecretAccessKey, streamName);
    }
}