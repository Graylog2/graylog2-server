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
package org.graylog.events.processor.modifier;

import com.floreysoft.jmte.Engine;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import jakarta.inject.Inject;
import org.graylog.events.event.Event;
import org.graylog.events.event.EventWithContext;
import org.graylog.events.processor.EventDefinition;

import java.util.Objects;

import static org.graylog.events.event.EventDto.FIELD_EVENT_DEFINITION_ID;
import static org.graylog.events.notifications.EventNotificationModelData.FIELD_EVENT_DEFINITION_DESCRIPTION;
import static org.graylog.events.notifications.EventNotificationModelData.FIELD_EVENT_DEFINITION_TITLE;
import static org.graylog.events.notifications.EventNotificationModelData.FIELD_EVENT_DEFINITION_TYPE;

public class EventSummaryModifier implements EventModifier {
    private final Engine templateEngine;

    @Inject
    public EventSummaryModifier(Engine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Override
    public void accept(EventWithContext eventWithContext, EventDefinition eventDefinition) throws EventModifierException {
        if (!Strings.isNullOrEmpty(eventDefinition.eventSummaryTemplate())) {
            final ImmutableMap.Builder<String, Object> dataModelBuilder = ImmutableMap.builder();

            dataModelBuilder.put(FIELD_EVENT_DEFINITION_ID, Objects.requireNonNull(eventDefinition.id()));
            dataModelBuilder.put(FIELD_EVENT_DEFINITION_TITLE, eventDefinition.title());
            dataModelBuilder.put(FIELD_EVENT_DEFINITION_TYPE, eventDefinition.config().type());
            dataModelBuilder.put(FIELD_EVENT_DEFINITION_DESCRIPTION, eventDefinition.description());
            
            if (eventWithContext.messageContext().isPresent()) {
                dataModelBuilder.put("source", eventWithContext.messageContext().get().getFields());
            } else if (eventWithContext.eventContext().isPresent()) {
                dataModelBuilder.put("source", eventWithContext.eventContext().get().toDto().fields());
            }

            final ImmutableMap<String, Object> dataModel = dataModelBuilder.build();

            final Event event = eventWithContext.event();
            event.setMessage(templateEngine.transform(eventDefinition.eventSummaryTemplate(), dataModel));
        }
    }
}
