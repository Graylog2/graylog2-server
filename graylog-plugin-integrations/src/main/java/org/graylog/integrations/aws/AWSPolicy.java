package org.graylog.integrations.aws;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

import java.util.List;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
// Define a JSON field order matching AWS examples. This improves readability.
@JsonPropertyOrder({AWSPolicy.VERSION, AWSPolicy.STATEMENT})
public abstract class AWSPolicy {

    public static final String VERSION = "Version";
    public static final String STATEMENT = "Statement";

    @JsonProperty(VERSION)
    public abstract String version();

    @JsonProperty(STATEMENT)
    public abstract List<AWSPolicyStatement> statement();

    public static AWSPolicy create(@JsonProperty(VERSION) String version,
                                   @JsonProperty(STATEMENT) List<AWSPolicyStatement> statement) {
        return new AutoValue_AWSPolicy(version, statement);
    }
}