package org.graylog2.rest.models.system.loggers.responses;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
@JsonAutoDetect
public abstract class LogMessage {

    @JsonProperty
    public abstract String message();

    @JsonProperty("class_name")
    public abstract String className();

    @JsonProperty
    public abstract String level();

    @JsonProperty
    public abstract String timestamp();

    @JsonProperty
    public abstract List<String> throwable();

    @JsonCreator
    public static LogMessage create(@JsonProperty("message") String message,
                                    @JsonProperty("class_name") String className,
                                    @JsonProperty("level") String level,
                                    @JsonProperty("timestamp") String timestamp,
                                    @JsonProperty("throwable") List<String> throwable) {
        return new AutoValue_LogMessage(message, className, level, timestamp, throwable);
    }

}
