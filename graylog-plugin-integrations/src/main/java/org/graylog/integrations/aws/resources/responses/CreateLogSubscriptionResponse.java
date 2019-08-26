package org.graylog.integrations.aws.resources.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class CreateLogSubscriptionResponse {

    private static final String RESULT = "result";

    @JsonProperty(RESULT)
    public abstract String result();

    public static CreateLogSubscriptionResponse create(@JsonProperty(RESULT) String result) {
        return new AutoValue_CreateLogSubscriptionResponse(result);
    }
}