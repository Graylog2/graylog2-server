package org.graylog.events.contentpack.entities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog2.contentpacks.model.entities.references.ReferenceMap;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
@JsonDeserialize(builder = EventNotificationHandlerConfigEntity.Builder.class)
public abstract class EventNotificationHandlerConfigEntity {

    private static final String FIELD_NOTIFICATION_ID = "notification_id";
    private static final String FIELD_NOTIFICATION_PARAMETERS = "notification_parameters";

    @JsonProperty(FIELD_NOTIFICATION_ID)
    public abstract ValueReference notificationId();

    @JsonProperty(FIELD_NOTIFICATION_PARAMETERS)
    public abstract Optional<ReferenceMap> notificationParameters();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {

        public static Builder create() {
            return new AutoValue_EventNotificationHandlerConfigEntity.Builder();
        }

        @JsonProperty(FIELD_NOTIFICATION_ID)
        public abstract Builder notificationId(ValueReference notificationId);

        @JsonProperty(FIELD_NOTIFICATION_PARAMETERS)
        public abstract Builder notificationParameters(@Nullable ReferenceMap notificationParameters);

        public abstract EventNotificationHandlerConfigEntity build();
    }
}
