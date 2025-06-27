package org.graylog.integrations.dbconnector.api.requests;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;

@JsonAutoDetect
@AutoValue
@JsonDeserialize(builder = DBConnectorRequestImpl.Builder.class)
public abstract class DBConnectorRequestImpl implements DBConnectorRequest {

    public static Builder builder() {
        return Builder.create();
    }

    @AutoValue.Builder
    public static abstract class Builder implements DBConnectorRequest.Builder<Builder> {
        @JsonCreator
        public static Builder create() {
            return new AutoValue_DBConnectorRequestImpl.Builder();
        }

        public abstract DBConnectorRequestImpl build();
    }
}
