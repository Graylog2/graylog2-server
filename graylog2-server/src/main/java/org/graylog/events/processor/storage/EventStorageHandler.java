/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
