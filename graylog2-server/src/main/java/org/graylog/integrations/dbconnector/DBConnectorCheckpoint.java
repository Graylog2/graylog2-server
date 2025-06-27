package org.graylog.integrations.dbconnector;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.inputs.persistence.InputStateData;

@AutoValue
@JsonAutoDetect
@JsonTypeName(DBConnectorCheckpoint.NAME)
@JsonDeserialize(builder = DBConnectorCheckpoint.Builder.class)
public abstract class DBConnectorCheckpoint implements InputStateData {
    public static final String NAME = "dbconnector_checkpoint";
    private static final String FIELD_LAST_EVENT = "last_event_time";

    @JsonProperty(FIELD_LAST_EVENT)
    public abstract String lastEventTime();

    public static Builder builder() {
        return new AutoValue_DBConnectorCheckpoint.Builder().type(NAME);
    }

    @AutoValue.Builder
    public abstract static class Builder implements InputStateData.Builder<Builder> {

        @JsonCreator
        public static Builder create() {
            return DBConnectorCheckpoint.builder();
        }

        @JsonProperty(FIELD_LAST_EVENT)
        public abstract Builder lastEventTime(String timestamp);

        public abstract DBConnectorCheckpoint build();
    }
}
