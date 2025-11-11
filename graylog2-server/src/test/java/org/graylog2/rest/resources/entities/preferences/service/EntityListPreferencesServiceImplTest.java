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

import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.rest.resources.entities.preferences.model.EntityListPreferences;
import org.graylog2.rest.resources.entities.preferences.model.SortPreferences;
import org.graylog2.rest.resources.entities.preferences.model.StoredEntityListPreferences;
import org.graylog2.rest.resources.entities.preferences.model.StoredEntityListPreferencesId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog2.rest.resources.entities.preferences.model.SortPreferences.SortOrder.ASC;
import static org.graylog2.rest.resources.entities.preferences.model.SortPreferences.SortOrder.DESC;

@ExtendWith(MongoDBExtension.class)
public class EntityListPreferencesServiceImplTest {
    private static final StoredEntityListPreferencesId existingId = StoredEntityListPreferencesId.builder()
            .entityListId("list")
            .userId("user")
            .build();

    private static final StoredEntityListPreferencesId wrongId = StoredEntityListPreferencesId.builder()
            .entityListId("blahblah")
            .userId("user")
            .build();

    private EntityListPreferencesService toTest;

    @BeforeEach
    public void setUp(MongoCollections mongoCollections) {
        this.toTest = new EntityListPreferencesServiceImpl(mongoCollections);
    }

    @Test
    public void returnsNullWhenFetchingPreferenceFromEmptyDB() {
        final StoredEntityListPreferences storedEntityListPreferences = toTest.get(wrongId);
        assertThat(storedEntityListPreferences).isNull();
    }

    @Test
    public void performsSaveAndGetOperationsCorrectly() {
        final StoredEntityListPreferences existingPreference = StoredEntityListPreferences.builder()
                .preferencesId(existingId)
                .preferences(EntityListPreferences.create(List.of("title", "description"), 42, new SortPreferences("title", ASC)))
                .build();

        //save
        boolean saved = toTest.save(existingPreference);
        assertThat(saved).isTrue();

        //check save
        StoredEntityListPreferences storedEntityListPreferences = toTest.get(existingId);
        assertThat(existingPreference).isEqualTo(storedEntityListPreferences);

        //check wrong does not exist
        storedEntityListPreferences = toTest.get(wrongId);
        assertThat(storedEntityListPreferences).isNull();

        //update with save
        StoredEntityListPreferences updatedPreference = StoredEntityListPreferences.builder()
                .preferencesId(existingId)
                .preferences(EntityListPreferences.create(List.of("title", "description", "owner"), 13, new SortPreferences("title", DESC)))
                .build();
        saved = toTest.save(updatedPreference);
        assertThat(saved).isTrue();

        //check update with save
        storedEntityListPreferences = toTest.get(existingId);
        assertThat(updatedPreference).isEqualTo(storedEntityListPreferences);

        //update with some values cleaned
        updatedPreference = StoredEntityListPreferences.builder()
                .preferencesId(existingId)
                .preferences(EntityListPreferences.create(List.of("title", "description", "owner"), null, null))
                .build();
        saved = toTest.save(updatedPreference);
        assertThat(saved).isTrue();

        //check update with save
        storedEntityListPreferences = toTest.get(existingId);
        assertThat(updatedPreference).isEqualTo(storedEntityListPreferences);
    }

}
