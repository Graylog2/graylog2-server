package org.graylog.plugins.views.search.searchtypes.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.DateTime;

import java.util.Set;

public interface CommonEventSummary {
    String FIELD_ID = "id";
    String FIELD_STREAMS = "streams";
    String FIELD_EVENT_TIMESTAMP = "timestamp";
    String FIELD_MESSAGE = "message";
    String FIELD_ALERT = "alert";
    String FIELD_EVENT_DEFINITION_ID = "event_definition_id";

    @JsonProperty(FIELD_ID)
    String id();

    @JsonProperty(FIELD_STREAMS)
    Set<String> streams();

    @JsonProperty(FIELD_EVENT_TIMESTAMP)
    DateTime timestamp();

    @JsonProperty(FIELD_MESSAGE)
    String message();

    @JsonProperty(FIELD_ALERT)
    boolean alert();

    @JsonProperty(FIELD_EVENT_DEFINITION_ID)
    String eventDefinitionId();
}
