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
package org.graylog.events.fields;

import org.graylog.events.event.Event;
import org.graylog.events.event.EventWithContext;
import org.graylog.events.fields.providers.FieldValueProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;

@Singleton
public class EventFieldSpecEngine {
    private static final Logger LOG = LoggerFactory.getLogger(EventFieldSpecEngine.class);

    private final Map<String, FieldValueProvider.Factory> fieldValueProviders;

    @Inject
    public EventFieldSpecEngine(Map<String, FieldValueProvider.Factory> fieldValueProviders) {
        this.fieldValueProviders = fieldValueProviders;
    }

    public void execute(List<EventWithContext> eventsWithContext, Map<String, EventFieldSpec> fieldSpec) {
        for (final Map.Entry<String, EventFieldSpec> entry : fieldSpec.entrySet()) {
            final String fieldName = entry.getKey();
            final EventFieldSpec spec = entry.getValue();

            for (final FieldValueProvider.Config providerConfig : spec.providers()) {
                final FieldValueProvider.Factory providerFactory = fieldValueProviders.get(providerConfig.type());

                if (providerFactory == null) {
                    LOG.error("Couldn't find field provider factory for type {}", providerConfig.type());
                    continue;
                }

                final FieldValueProvider provider = providerFactory.create(providerConfig);

                for (final EventWithContext eventWithContext : eventsWithContext) {
                    final Event event = eventWithContext.event();
                    event.setField(fieldName, provider.get(fieldName, eventWithContext));
                }
            }
        }
    }
}
