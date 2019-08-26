package org.graylog.integrations.aws.cloudwatch;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.joda.time.DateTime;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class KinesisLogEntry {

    private static final String KINESIS_STREAM = "kinesis_stream";
    private static final String LOG_GROUP = "log_group";
    private static final String LOG_STREAM = "log_stream";
    private static final String TIMESTAMP = "timestamp";
    private static final String MESSAGE = "message";

    @JsonProperty(KINESIS_STREAM)
    public abstract String kinesisStream();

    /**
     * CloudWatch Log Group and Log Stream are optional, since messages may have been written directly to Kinesis
     * without using CloudWatch. Only CloudWatch messages written VIA Kinesis CloudWatch subscriptions will
     * contain a log group and stream.
     */
    @JsonProperty(LOG_GROUP)
    public abstract String logGroup();

    @JsonProperty(LOG_STREAM)
    public abstract String logStream();

    @JsonProperty(TIMESTAMP)
    public abstract DateTime timestamp();

    @JsonProperty(MESSAGE)
    public abstract String message();

    @JsonCreator
    public static KinesisLogEntry create(@JsonProperty(KINESIS_STREAM) String kinesisStream,
                                         @JsonProperty(LOG_GROUP) String logGroup,
                                         @JsonProperty(LOG_STREAM) String logStream,
                                         @JsonProperty(TIMESTAMP) DateTime timestamp,
                                         @JsonProperty(MESSAGE) String message) {
        return new AutoValue_KinesisLogEntry(kinesisStream, logGroup, logStream, timestamp, message);
    }
}