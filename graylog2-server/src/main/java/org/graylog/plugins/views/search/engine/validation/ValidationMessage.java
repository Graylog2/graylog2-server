package org.graylog.plugins.views.search.engine.validation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AutoValue
public abstract class ValidationMessage {

    @JsonProperty
    @Nullable
    public abstract Integer line();
    @JsonProperty
    @Nullable
    public abstract Integer column();
    @JsonProperty
    @Nullable
    public abstract String errorType();
    @JsonProperty
    public abstract String errorMessage();

    public static Builder builder() {
        return new AutoValue_ValidationMessage.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder line(int line);
        public abstract Builder column(int column);
        public abstract Builder errorType(@Nullable String errorType);
        public abstract Builder errorMessage(String errorMessage);

        public abstract ValidationMessage build();
    }

}
