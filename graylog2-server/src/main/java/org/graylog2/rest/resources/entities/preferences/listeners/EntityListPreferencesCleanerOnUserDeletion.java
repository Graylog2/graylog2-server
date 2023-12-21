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
package org.graylog2.rest.resources.entities.preferences.listeners;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog2.rest.resources.entities.preferences.service.EntityListPreferencesService;
import org.graylog2.users.events.UserDeletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class EntityListPreferencesCleanerOnUserDeletion {
    private static final Logger LOG = LoggerFactory.getLogger(EntityListPreferencesCleanerOnUserDeletion.class);

    private final EntityListPreferencesService entityListPreferencesService;

    @Inject
    public EntityListPreferencesCleanerOnUserDeletion(final EventBus eventBus,
                                                      final EntityListPreferencesService entityListPreferencesService) {
        this.entityListPreferencesService = entityListPreferencesService;
        eventBus.register(this);
    }

    @Subscribe
    public void handleUserDeletedEvent(final UserDeletedEvent event) {
        LOG.debug("Removing entity list preferences of user <{}/{}>",
                event.userName(), event.userId());
        entityListPreferencesService.deleteAllForUser(event.userId());
    }
}
