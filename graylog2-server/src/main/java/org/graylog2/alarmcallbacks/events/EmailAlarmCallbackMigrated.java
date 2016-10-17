package org.graylog2.alarmcallbacks.events;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.Map;
import java.util.Optional;

@AutoValue
@JsonAutoDetect
public abstract class EmailAlarmCallbackMigrated {
    private static final String FIELD_CALLBACK_IDS = "callback_ids";

    @JsonProperty(FIELD_CALLBACK_IDS)
    public abstract Map<String, Optional<String>> callbackIds();

    @JsonCreator
    public static EmailAlarmCallbackMigrated create(@JsonProperty(FIELD_CALLBACK_IDS) Map<String, Optional<String>> callbackIds) {
        return new AutoValue_EmailAlarmCallbackMigrated(callbackIds);
    }
}
