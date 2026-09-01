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

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.shiro.subject.Subject;
import org.bson.types.ObjectId;
import org.graylog.events.configuration.EventsConfiguration;
import org.graylog.events.configuration.EventsConfigurationProvider;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog2.cluster.ClusterConfigChangedEvent;
import org.graylog2.shared.utilities.AutoValueUtils;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.graylog2.shared.security.RestPermissions.EVENT_DEFINITIONS_READ;

/**
 * Resolves the {@link EventDefinitionFilter} to apply to a subject's event searches.
 * <p>
 * Stateless with respect to permissions and safe to share: permissions are evaluated inside
 * {@link #forSubject(Subject)} on every call, so grant changes (for example removing a collaborator from an
 * event definition) take effect immediately.
 * <p>
 * The enforcement setting is the one thing held between calls. It is read once and then refreshed on
 * {@link ClusterConfigChangedEvent}, so it is never stale and costs one MongoDB read per configuration
 * change rather than one per event search.
 */
@Singleton
public class EventDefinitionFilterFactory {
    private static final String EVENTS_CONFIGURATION_TYPE =
            AutoValueUtils.getCanonicalName(EventsConfiguration.class);

    private final EventsConfigurationProvider eventsConfigurationProvider;
    private final AtomicReference<EventsConfiguration> eventsConfiguration;
    private final DBEventDefinitionService eventDefinitionService;

    @Inject
    public EventDefinitionFilterFactory(EventsConfigurationProvider eventsConfigurationProvider,
                                        DBEventDefinitionService eventDefinitionService,
                                        EventBus serverEventBus) {
        this(eventsConfigurationProvider, eventDefinitionService);
        serverEventBus.register(this);
    }

    EventDefinitionFilterFactory(EventsConfigurationProvider eventsConfigurationProvider,
                                 DBEventDefinitionService eventDefinitionService) {
        this.eventsConfigurationProvider = eventsConfigurationProvider;
        this.eventDefinitionService = eventDefinitionService;
        this.eventsConfiguration = new AtomicReference<>(eventsConfigurationProvider.get());
    }

    @Subscribe
    public void handleUpdatedClusterConfig(ClusterConfigChangedEvent event) {
        if (EVENTS_CONFIGURATION_TYPE.equals(event.type())) {
            eventsConfiguration.set(eventsConfigurationProvider.get());
        }
    }

    public EventDefinitionFilter forSubject(Subject subject) {
        if (!eventsConfiguration.get().enforceEventDefinitionPermissions() || subject.isPermitted(EVENT_DEFINITIONS_READ)) {
            return EventDefinitionFilter.allAllowed();
        }
        final Set<String> ids = eventDefinitionService.findPermittedIds(
                        id -> subject.isPermitted(EVENT_DEFINITIONS_READ + ":" + id)
                ).stream()
                .map(ObjectId::toHexString)
                .collect(Collectors.toUnmodifiableSet());
        return EventDefinitionFilter.allowList(ids);
    }
}
