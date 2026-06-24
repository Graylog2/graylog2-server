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
package org.graylog.events.search;

import jakarta.inject.Inject;
import org.apache.shiro.subject.Subject;
import org.graylog.events.configuration.EventsConfigurationProvider;
import org.graylog.events.processor.DBEventDefinitionService;

import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.shared.security.RestPermissions.EVENT_DEFINITIONS_READ;

public class EventDefinitionFilterFactory {
    private final EventsConfigurationProvider eventsConfiguration;
    private final DBEventDefinitionService eventDefinitionService;

    @Inject
    public EventDefinitionFilterFactory(EventsConfigurationProvider eventsConfiguration,
                                        DBEventDefinitionService eventDefinitionService) {
        this.eventsConfiguration = eventsConfiguration;
        this.eventDefinitionService = eventDefinitionService;
    }

    public EventDefinitionFilter forSubject(Subject subject) {
        if (!eventsConfiguration.get().enforceEventDefinitionPermissions() || subject.isPermitted(EVENT_DEFINITIONS_READ)) {
            return EventDefinitionFilter.allAllowed();
        }
        final Set<String> ids = eventDefinitionService.findPermittedIds(
                        id -> subject.isPermitted(EVENT_DEFINITIONS_READ + ":" + id)
                ).stream()
                .map(org.bson.types.ObjectId::toHexString)
                .collect(Collectors.toSet());
        return EventDefinitionFilter.allowList(ids);
    }
}
