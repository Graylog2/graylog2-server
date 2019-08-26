package org.graylog.integrations.aws.resources.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class KinesisFullSetupRequest implements AWSRequest {

    private static final String ROLE_NAME = "role_name";
    private static final String LOG_GROUP_NAME = "log_group_name";
    private static final String STREAM_NAME = "stream_name";
    private static final String ROLE_POLICY_NAME = "role_policy_name";
    private static final String FILTER_NAME = "filter_name";
    private static final String FILTER_PATTERN = "filter_pattern";

    @JsonProperty(REGION)
    public abstract String region();

    @JsonProperty(AWS_ACCESS_KEY_ID)
    public abstract String awsAccessKeyId();

    @JsonProperty(AWS_SECRET_ACCESS_KEY)
    public abstract String awsSecretAccessKey();

    @JsonProperty(ROLE_NAME)
    public abstract String roleName();

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

    @JsonCreator
    public static KinesisFullSetupRequest create(@JsonProperty(REGION) String region,
                                                 @JsonProperty(AWS_ACCESS_KEY_ID) String awsAccessKeyId,
                                                 @JsonProperty(AWS_SECRET_ACCESS_KEY) String awsSecretAccessKey,
                                                 @JsonProperty(ROLE_NAME) String roleName,
                                                 @JsonProperty(LOG_GROUP_NAME) String getLogGroupName,
                                                 @JsonProperty(STREAM_NAME) String streamName,
                                                 @JsonProperty(ROLE_POLICY_NAME) String rolePolicyName,
                                                 @JsonProperty(FILTER_NAME) String filterName,
                                                 @JsonProperty(FILTER_PATTERN) String filterPattern) {
        return new AutoValue_KinesisFullSetupRequest(region, awsAccessKeyId, awsSecretAccessKey, roleName, getLogGroupName, streamName, rolePolicyName, filterName, filterPattern);
    }
}