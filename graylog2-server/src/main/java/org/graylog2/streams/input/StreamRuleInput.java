package org.graylog2.streams.input;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;

@AutoValue
@WithBeanGetter
public abstract class StreamRuleInput {

    @JsonProperty
    public abstract String title();

    @JsonProperty
    public abstract String name();

    @JsonProperty
    public abstract String id();

    public static Builder builder() {
        return new AutoValue_StreamRuleInput.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder title(String title);

        public abstract Builder name(String name);

        public abstract Builder id(String id);

        public abstract StreamRuleInput build();
    }

}
