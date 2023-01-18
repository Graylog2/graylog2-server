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

import org.graylog2.rest.resources.entities.preferences.model.StoredEntityListPreferences;

import java.util.HashMap;
import java.util.Map;

//TODO: to be replaced by MongoDB storage in a new collection, or session storage, or maybe even combination of both...no decision made yet
public class InMemoryEntityListPreferencesService implements EntityListPreferencesService {

    private static final Map<String, Map<String, StoredEntityListPreferences>> INMEMORY_STORAGE = new HashMap<>();

    @Override
    public StoredEntityListPreferences get(final String userId, final String entityListId) {
        final Map<String, StoredEntityListPreferences> userPreferences = INMEMORY_STORAGE.getOrDefault(userId, Map.of());
        return userPreferences.get(entityListId);
    }

    @Override
    public void save(final StoredEntityListPreferences preferences) {
        final Map<String, StoredEntityListPreferences> userPreferences = INMEMORY_STORAGE.getOrDefault(preferences.userId(), new HashMap<>());
        userPreferences.put(preferences.entityListId(), preferences);
        INMEMORY_STORAGE.put(preferences.userId(), userPreferences);
    }
}
