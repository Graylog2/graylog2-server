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
import org.graylog.events.event.EventWithContext;
import org.graylog.events.fields.FieldValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class FixedValueFieldValueProvider extends AbstractFieldValueProvider {
    public interface Factory extends AbstractFieldValueProvider.Factory<FixedValueFieldValueProvider> {
        @Override
        FixedValueFieldValueProvider create(FieldValueProvider.Config config);
    }

    private static final Logger LOG = LoggerFactory.getLogger(FixedValueFieldValueProvider.class);

    private final Config config;

    @Inject
    public FixedValueFieldValueProvider(@Assisted FieldValueProvider.Config config) {
        super(config);
        this.config = (Config) config;
    }

    @Override
    protected FieldValue doGet(String fieldName, EventWithContext eventWithContext) {
        return FieldValue.string(config.value());
    }

    @AutoValue
    @JsonTypeName(Config.TYPE_NAME)
    @JsonDeserialize(builder = Config.Builder.class)
    public static abstract class Config implements AbstractFieldValueProvider.Config {
        public static final String TYPE_NAME = "fixedvalue-v1";

        private static final String FIELD_VALUE = "value";

        @JsonProperty(FIELD_VALUE)
        public abstract String value();

        public static Builder builder() {
            return Builder.create();
        }

        public abstract Builder toBuilder();

        @AutoValue.Builder
        public static abstract class Builder implements FieldValueProvider.Config.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_FixedValueFieldValueProvider_Config.Builder().type(TYPE_NAME);
            }

            @JsonProperty(FIELD_VALUE)
            public abstract Builder value(String value);

            public abstract Config build();
        }
    }
}
