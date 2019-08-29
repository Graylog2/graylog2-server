package org.graylog.integrations.aws.resources.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class CreateLogSubscriptionRequest implements AWSRequest {

    private static final String LOG_GROUP_NAME = "log_group_name";
    private static final String FILTER_NAME = "filter_name";
    private static final String FILTER_PATTERN = "filter_pattern";
    private static final String DESTINATION_STREAM_ARN = "destination_stream_arn";
    private static final String ROLE_ARN = "role_arn";

    @JsonProperty(REGION)
    public abstract String region();

    @JsonProperty(AWS_ACCESS_KEY_ID)
    public abstract String awsAccessKeyId();

    @JsonProperty(AWS_SECRET_ACCESS_KEY)
    public abstract String awsSecretAccessKey();

    /**
     * {@see <a href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/SubscriptionFilters.html">CloudWatch Subscription Filter</a>},
     *
     * @return
     */
    @JsonProperty(LOG_GROUP_NAME)
    public abstract String logGroupName();

    @JsonProperty(FILTER_NAME)
    public abstract String filterName();

    @JsonProperty(FILTER_PATTERN)
    public abstract String filterPattern();

    @JsonProperty(DESTINATION_STREAM_ARN)
    public abstract String destinationStreamArn();

    @JsonProperty(ROLE_ARN)
    public abstract String getRoleArn();

    @JsonCreator
    public static CreateLogSubscriptionRequest create(@JsonProperty(REGION) String region,
                                                      @JsonProperty(AWS_ACCESS_KEY_ID) String awsAccessKeyId,
                                                      @JsonProperty(AWS_SECRET_ACCESS_KEY) String awsSecretAccessKey,
                                                      @JsonProperty(LOG_GROUP_NAME) String getLogGroupName,
                                                      @JsonProperty(FILTER_NAME) String filterName,
                                                      @JsonProperty(FILTER_PATTERN) String filterPattern,
                                                      @JsonProperty(DESTINATION_STREAM_ARN) String destinationStreamArn,
                                                      @JsonProperty(ROLE_ARN) String getRoleArn) {
        return new AutoValue_CreateLogSubscriptionRequest(region, awsAccessKeyId, awsSecretAccessKey, getLogGroupName, filterName, filterPattern, destinationStreamArn, getRoleArn);
    }
}