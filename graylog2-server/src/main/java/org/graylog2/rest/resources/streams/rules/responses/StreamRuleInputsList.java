package org.graylog2.rest.resources.streams.rules.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.streams.input.StreamRuleInput;

import java.util.Set;

@AutoValue
@WithBeanGetter
public abstract class StreamRuleInputsList {

    @JsonProperty("inputs")
    public abstract Set<StreamRuleInput> inputs();

    public static StreamRuleInputsList create(Set<StreamRuleInput> inputs) {
        return new AutoValue_StreamRuleInputsList(inputs);
    }
}
