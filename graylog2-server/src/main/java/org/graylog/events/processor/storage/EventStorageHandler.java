package org.graylog.events.processor.storage;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.graylog.events.event.EventWithContext;

import java.util.List;

public interface EventStorageHandler {
    interface Factory<TYPE extends EventStorageHandler> {
        TYPE create(Config config);
    }

    void handleEvents(List<EventWithContext> eventsWithContext);

    EventStorageHandlerCheckResult checkPreconditions();

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXISTING_PROPERTY,
            property = Config.TYPE_FIELD,
            visible = true,
            defaultImpl = Config.FallbackEventStorageHandlerConfig.class)
    interface Config {
        String TYPE_FIELD = "type";

        @JsonProperty(TYPE_FIELD)
        String type();

        interface Builder<SELF> {
            @JsonProperty(TYPE_FIELD)
            SELF type(String type);
        }

        class FallbackEventStorageHandlerConfig implements Config {
            @Override
            public String type() {
                throw new UnsupportedOperationException();
            }
        }
    }
}
