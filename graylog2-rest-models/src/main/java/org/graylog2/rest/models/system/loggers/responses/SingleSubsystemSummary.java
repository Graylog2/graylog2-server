package org.graylog2.rest.models.system.loggers.responses;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
public abstract class SingleSubsystemSummary {
    @JsonProperty
    public abstract String title();
    @JsonProperty
    public abstract String category();
    @JsonProperty
    public abstract String description();
    @JsonProperty
    public abstract String level();
    @JsonProperty("level_syslog")
    public abstract int levelSyslog();

    @JsonCreator
    public static SingleSubsystemSummary create(@JsonProperty("title") String title,
                                                @JsonProperty("category") String category,
                                                @JsonProperty("description") String description,
                                                @JsonProperty("level") String level,
                                                @JsonProperty("level_syslog") int levelSyslog) {
        return new AutoValue_SingleSubsystemSummary(title, category, description, level, levelSyslog);
    }
}
