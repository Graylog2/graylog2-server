package org.graylog2.rest.models.alarmcallbacks;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
@JsonAutoDetect
public abstract class AlarmCallbackListSummary {
    @JsonProperty
    public abstract int total();

    @JsonProperty("alarmcallbacks")
    public abstract List<AlarmCallbackSummary> alarmCallbacks();

    @JsonCreator
    public static AlarmCallbackListSummary create(@JsonProperty("total") int total, @JsonProperty("alarmcallbacks") List<AlarmCallbackSummary> alarmCallbacks) {
        return new AutoValue_AlarmCallbackListSummary(total, alarmCallbacks);
    }

    public static AlarmCallbackListSummary create(List<AlarmCallbackSummary> alarmCallbacks) {
        return new AutoValue_AlarmCallbackListSummary(alarmCallbacks.size(), alarmCallbacks);
    }
}
