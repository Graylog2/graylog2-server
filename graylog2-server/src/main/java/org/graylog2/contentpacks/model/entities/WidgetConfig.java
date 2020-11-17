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
package org.graylog2.contentpacks.model.entities;

import org.graylog2.contentpacks.exceptions.ContentPackException;
import org.graylog2.contentpacks.model.entities.references.ReferenceMap;
import org.graylog2.contentpacks.model.entities.references.ReferenceMapUtils;
import org.graylog2.contentpacks.model.entities.references.ValueReference;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class WidgetConfig {
    private Map<String, Object> config;

    public WidgetConfig(ReferenceMap config, Map<String, ValueReference> parameters) {
        this.config = ReferenceMapUtils.toValueMap(config, parameters);
    }

    public Optional<String> getOptionalString(String key) {
        Object value = config.get(key);
        if (value instanceof String) {
            return Optional.of((String) value);
        }
        return Optional.empty();
    }

    public String getString(String key) throws ContentPackException {
        Object value = config.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        throw new ContentPackException("Could not find key " + key + " in config.");
    }

    public boolean getBoolean(String key) throws ContentPackException {
        Object value = config.get(key);
        if (value != null) {
            return (boolean) value;
        }
        throw new ContentPackException("Could not find key " + key + " in config.");
    }

    public Optional<Boolean> getOptionalBoolean(String key) {
        final Boolean value = (Boolean)config.get(key);
        return Optional.ofNullable(value);
    }

    public int getInteger(String key) {
        Object value = config.get(key);
        if (value != null) {
            return (int) value;
        }
        throw new ContentPackException("Could not find key " + key + " in config.");
    }

    public Optional<Integer> getOptionalInteger(String key) {
        Object value = config.get(key);
        if (value instanceof Integer) {
            return Optional.of((Integer) value);
        }
        return Optional.empty();
    }

    public List<Object> getList(String key) {
        Object value = config.get(key);
        if (value instanceof List) {
            return (List<Object>) value;
        }
        return Collections.emptyList();
    }
}
