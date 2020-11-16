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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.inject.assistedinject.Assisted;
import org.graylog.events.event.Event;
import org.graylog.events.event.EventWithContext;
import org.graylog.events.fields.FieldValue;
import org.graylog2.lookup.LookupTableService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.lookup.LookupResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class LookupTableFieldValueProvider extends AbstractFieldValueProvider {
    public interface Factory extends AbstractFieldValueProvider.Factory<LookupTableFieldValueProvider> {
        @Override
        LookupTableFieldValueProvider create(FieldValueProvider.Config config);
    }

    private static final Logger LOG = LoggerFactory.getLogger(LookupTableFieldValueProvider.class);

    private final Config config;
    private final LookupTableService lookupTableService;

    @Inject
    public LookupTableFieldValueProvider(@Assisted FieldValueProvider.Config config,
                                         LookupTableService lookupTableService) {
        super(config);
        this.config = (Config) config;
        this.lookupTableService = lookupTableService;
    }

    @Override
    protected FieldValue doGet(String fieldName, EventWithContext eventWithContext) {
        if (!lookupTableService.hasTable(config.tableName())) {
            throw new IllegalArgumentException("Lookup-table doesn't exist: " + config.tableName());
        }

        final LookupTableService.Function function = lookupTableService.newBuilder()
                .lookupTable(config.tableName())
                .build();

        if (eventWithContext.messageContext().isPresent()) {
            final Message message = eventWithContext.messageContext().get();

            return lookup(function, message.getField(config.keyField()));
        } else if (eventWithContext.eventContext().isPresent()) {
            final Event event = eventWithContext.eventContext().get();

            return lookup(function, event.getField(config.keyField()).value());
        } else {
            throw new IllegalStateException("Neither an event nor a message context exists in event: " + eventWithContext.toString());
        }
    }

    private FieldValue lookup(LookupTableService.Function function, Object keyValue) {
        try {
            final LookupResult result = function.lookup(keyValue);
            if (result == null || result.isEmpty()) {
                // TODO: Should we raise an exception here?
                return FieldValue.error();
            }
            return FieldValue.string(String.valueOf(result.singleValue()));
        } catch (Exception e) {
            LOG.error("Couldn't lookup value for key <{}/{}>", config.keyField(), keyValue, e);
            return FieldValue.error();
        }
    }

    @AutoValue
    @JsonTypeName(Config.TYPE_NAME)
    @JsonDeserialize(builder = Config.Builder.class)
    public static abstract class Config implements AbstractFieldValueProvider.Config {
        public static final String TYPE_NAME = "lookup-v1";

        private static final String FIELD_TABLE_NAME = "table_name";
        private static final String FIELD_KEY_FIELD = "key_field";

        @JsonProperty(FIELD_TABLE_NAME)
        public abstract String tableName();

        @JsonProperty(FIELD_KEY_FIELD)
        public abstract String keyField();

        public static Builder builder() {
            return Builder.create();
        }

        public abstract Builder toBuilder();

        @AutoValue.Builder
        public static abstract class Builder implements FieldValueProvider.Config.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_LookupTableFieldValueProvider_Config.Builder().type(TYPE_NAME);
            }

            @JsonProperty(FIELD_TABLE_NAME)
            public abstract Builder tableName(String tableName);

            @JsonProperty(FIELD_KEY_FIELD)
            public abstract Builder keyField(String keyField);

            public abstract Config build();
        }
    }
}
