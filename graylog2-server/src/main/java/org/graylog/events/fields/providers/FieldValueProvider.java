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
package org.graylog.events.fields.providers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.graylog.events.event.EventWithContext;
import org.graylog.events.fields.FieldValue;

public interface FieldValueProvider {
    interface Factory<TYPE extends FieldValueProvider> {
        TYPE create(Config config);
    }

    FieldValue get(String fieldName, EventWithContext eventWithContext);

    @JsonTypeInfo(
            use = JsonTypeInfo.Id.NAME,
            include = JsonTypeInfo.As.EXISTING_PROPERTY,
            property = Config.TYPE_FIELD,
            visible = true,
            defaultImpl = Config.FallbackActionConfig.class)
    interface Config {
        String TYPE_FIELD = "type";

        @JsonProperty(TYPE_FIELD)
        String type();

        interface Builder<SELF> {
            @JsonProperty(TYPE_FIELD)
            SELF type(String type);
        }

        class FallbackActionConfig implements Config {
            @Override
            public String type() {
                throw new UnsupportedOperationException();
            }
        }
    }
}
