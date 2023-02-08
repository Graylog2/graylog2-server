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

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.util.concurrent.MoreExecutors;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.rest.resources.entities.preferences.model.EntityListPreferences;
import org.graylog2.rest.resources.entities.preferences.model.StoredEntityListPreferences;
import org.graylog2.rest.resources.entities.preferences.model.StoredEntityListPreferencesId;
import org.graylog2.rest.resources.entities.preferences.service.EntityListPreferencesService;
import org.graylog2.rest.resources.entities.preferences.service.EntityListPreferencesServiceImpl;
import org.graylog2.users.events.UserDeletedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
@ExtendWith(MockitoExtension.class)
class EntityListPreferencesCleanerOnUserDeletionTest {
    private AsyncEventBus eventBus;
    private EntityListPreferencesService service;
    @SuppressWarnings("unused")
    private EntityListPreferencesCleanerOnUserDeletion listener;

    @BeforeEach
    void setUp(MongoDBTestService mongodb,
               MongoJackObjectMapperProvider objectMapperProvider) {
        this.eventBus = new AsyncEventBus(MoreExecutors.directExecutor());
        this.service = Mockito.spy(new EntityListPreferencesServiceImpl(mongodb.mongoConnection(), objectMapperProvider));
        this.listener = new EntityListPreferencesCleanerOnUserDeletion(eventBus, service);
    }

    @Test
    void handlesUserDeletedEvent() {
        final String userId = "5f7b3b42f215bd12c3bfd937";

        //insert some preferences
        final StoredEntityListPreferencesId preferenceId1 = StoredEntityListPreferencesId.builder()
                .userId(userId)
                .entityListId("List 1")
                .build();
        service.save(StoredEntityListPreferences.builder()
                .preferencesId(preferenceId1)
                .preferences(new EntityListPreferences(List.of(), 42, null))
                .build());

        final StoredEntityListPreferencesId preferenceId2 = StoredEntityListPreferencesId.builder()
                .userId(userId)
                .entityListId("List 2")
                .build();
        service.save(StoredEntityListPreferences.builder()
                .preferencesId(preferenceId2)
                .preferences(new EntityListPreferences(List.of(), 42, null))
                .build());

        //verify they are present
        assertThat(service.get(preferenceId1)).isNotNull();
        assertThat(service.get(preferenceId2)).isNotNull();

        //post delete event for the user
        eventBus.post(UserDeletedEvent.create(userId, "deleted"));

        //verify preferences are gone
        assertThat(service.get(preferenceId1)).isNull();
        assertThat(service.get(preferenceId2)).isNull();

        //verify they were removed by proper service method
        verify(service).deleteAllForUser(userId);
    }
}
