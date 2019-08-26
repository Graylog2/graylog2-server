package org.graylog.integrations.aws.cloudwatch;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

/**
 * A single CloudWatch log event.
 * <p/>
 * Example payload:
 * <pre>
 * {
 *   "id": "33503748002479370955346306650196094071913271643270021120",
 *   "timestamp": 1502360020000,
 *   "message": "2 123456789 eni-aaaaaaaa 10.0.27.226 10.42.96.199 3604 17720 17 1 132 1502360020 1502360079 REJECT OK"
 * }
 * </pre>
 */
@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class CloudWatchLogEvent {

    private static final String ID = "id";
    private static final String TIMESTAMP = "timestamp";
    private static final String MESSAGE = "message";

    @JsonProperty(ID)
    public abstract String id(); // A very long sequence of digits stored as a String

    @JsonProperty(TIMESTAMP)
    public abstract long timestamp();

    @JsonProperty(MESSAGE)
    public abstract String message();

    @JsonCreator
    public static CloudWatchLogEvent create(@JsonProperty(ID) String id,
                                            @JsonProperty(TIMESTAMP) long timestamp,
                                            @JsonProperty(MESSAGE) String message) {
        return new AutoValue_CloudWatchLogEvent(id, timestamp, message);
    }
}
