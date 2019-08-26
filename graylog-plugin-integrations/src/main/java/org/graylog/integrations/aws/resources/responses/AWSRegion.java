package org.graylog.integrations.aws.resources.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class AWSRegion {

    private static final String VALUE = "value";
    private static final String LABEL = "label";

    // eu-west-2
    @JsonProperty(VALUE)
    public abstract String regionId();

    // The combination of both the name and description for display in the UI:
    // EU (London): eu-west-2
    @JsonProperty(LABEL)
    public abstract String displayValue();

    public static AWSRegion create(@JsonProperty(VALUE) String value,
                                   @JsonProperty(LABEL) String label) {
        return new AutoValue_AWSRegion(value, label);
    }
}