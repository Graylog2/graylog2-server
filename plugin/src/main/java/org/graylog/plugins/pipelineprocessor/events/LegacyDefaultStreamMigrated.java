package org.graylog.plugins.pipelineprocessor.events;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

@AutoValue
@JsonAutoDetect
public abstract class LegacyDefaultStreamMigrated {
    @JsonProperty
    public abstract boolean migrationDone();

    @JsonCreator
    public static LegacyDefaultStreamMigrated create(@JsonProperty("migration_done") boolean migrationDone) {
        return new AutoValue_LegacyDefaultStreamMigrated(migrationDone);
    }
}
