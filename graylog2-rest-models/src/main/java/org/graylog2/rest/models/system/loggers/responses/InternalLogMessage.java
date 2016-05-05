package org.graylog2.rest.models.system.loggers.responses;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@AutoValue
@JsonAutoDetect
public abstract class InternalLogMessage {

    @JsonProperty
    @NotEmpty
    public abstract String message();

    @JsonProperty("class_name")
    @NotEmpty
    public abstract String className();

    @JsonProperty
    @NotEmpty
    public abstract String level();

    @JsonProperty
    @NotNull
    public abstract DateTime timestamp();

    @JsonProperty
    @NotNull
    public abstract List<String> throwable();

    @JsonProperty("thread_name")
    @NotEmpty
    public abstract String threadName();

    @JsonProperty
    @Nullable
    public abstract String ndc();

    @JsonProperty
    @NotNull
    public abstract Map<String, String> mdc();

    @JsonCreator
    public static InternalLogMessage create(@JsonProperty("message") @NotEmpty String message,
                                            @JsonProperty("class_name") @NotEmpty String className,
                                            @JsonProperty("level") @NotEmpty String level,
                                            @JsonProperty("timestamp") @NotNull DateTime timestamp,
                                            @JsonProperty("throwable") @NotNull List<String> throwable,
                                            @JsonProperty("thread_name") @NotEmpty String threadName,
                                            @JsonProperty("ndc") @Nullable String ndc,
                                            @JsonProperty("mdc") @NotNull Map<String, String> mdc) {
        return new AutoValue_InternalLogMessage(message, className, level, timestamp, throwable, threadName, ndc, mdc);
    }

}
