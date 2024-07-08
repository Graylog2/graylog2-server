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
package org.graylog.security.certutil;

import org.graylog2.cluster.ClusterConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class InMemoryClusterConfigService implements ClusterConfigService {

    private final Map<String, Object> storage = new HashMap<>();

    @Override
    public <T> T get(Class<T> type) {
        return get(type.getCanonicalName(), type);
    }

    @Override
    public <T> T extractPayload(Object payload, Class<T> type) {
        throw new UnsupportedOperationException("not supported here");
    }

    @Override
    public <T> T get(String key, Class<T> type) {
        return (T) storage.get(key);
    }

    @Override
    public ClusterConfig getRaw(Class<?> key) {
        throw new UnsupportedOperationException("not supported here");
    }

    @Override
    public <T> T getOrDefault(Class<T> type, T defaultValue) {
        return Optional.ofNullable(get(type)).orElse(defaultValue);
    }

    @Override
    public <T> void write(String key, T payload) {
        storage.put(key, payload);
    }

    @Override
    public <T> void write(T payload) {
        storage.put(payload.getClass().getCanonicalName(), payload);
    }

    @Override
    public <T> int remove(Class<T> type) {
        final Object removed = storage.remove(type.getCanonicalName());
        return removed != null ? 1 : 0;
    }

    @Override
    public Set<Class<?>> list() {
        throw new UnsupportedOperationException("not supported here");
    }
}
