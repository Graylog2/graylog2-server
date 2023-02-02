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
package org.graylog2.rest.resources.entities.preferences.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.rest.resources.entities.preferences.model.EntityListPreferences;
import org.graylog2.rest.resources.entities.preferences.model.StoredEntityListPreferences;
import org.graylog2.rest.resources.entities.preferences.model.StoredEntityListPreferencesId;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EntityListPreferencesServiceImplTest {

    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private final StoredEntityListPreferencesId existingId = StoredEntityListPreferencesId.builder()
            .entityListId("list")
            .userId("user")
            .build();

    private final StoredEntityListPreferencesId wrongId = StoredEntityListPreferencesId.builder()
            .entityListId("blahblah")
            .userId("user")
            .build();

    private EntityListPreferencesService toTest;

    @Before
    public void setUp() {
        final MongoConnection mongoConnection = mongodb.mongoConnection();
        final MongoJackObjectMapperProvider objectMapperProvider = new MongoJackObjectMapperProvider(new ObjectMapper());
        this.toTest = new EntityListPreferencesServiceImpl(mongoConnection, objectMapperProvider);
    }

    @Test
    public void returnsNullWhenFetchingPreferenceFromEmptyDB() {
        final StoredEntityListPreferences storedEntityListPreferences = toTest.get(wrongId);
        assertNull(storedEntityListPreferences);
    }

    @Test
    public void performsSaveAndGetOperationsCorrectly() {
        final StoredEntityListPreferences existingPreference = StoredEntityListPreferences.builder()
                .preferencesId(existingId)
                .preferences(new EntityListPreferences(List.of("title", "description"), 42))
                .build();

        //save
        boolean saved = toTest.save(existingPreference);
        assertTrue(saved);

        //check save
        StoredEntityListPreferences storedEntityListPreferences = toTest.get(existingId);
        assertEquals(existingPreference, storedEntityListPreferences);

        //check wrong does not exist
        storedEntityListPreferences = toTest.get(wrongId);
        assertNull(storedEntityListPreferences);

        //update with save
        final StoredEntityListPreferences updatedPreference = StoredEntityListPreferences.builder()
                .preferencesId(existingId)
                .preferences(new EntityListPreferences(List.of("title", "description", "owner"), 13))
                .build();
        saved = toTest.save(updatedPreference);
        assertTrue(saved);

        //check update with save
        storedEntityListPreferences = toTest.get(existingId);
        assertEquals(updatedPreference, storedEntityListPreferences);
    }

}
