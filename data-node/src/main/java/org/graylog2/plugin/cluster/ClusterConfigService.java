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
package org.graylog2.plugin.cluster;

import java.util.Set;

/**
 * Service to save and retrieve cluster configuration beans.
 */
public interface ClusterConfigService {
    /**
     * Retrieve Java class of a certain type from the cluster configuration.
     *
     * @param type The {@link Class} of the Java configuration bean to retrieve.
     * @param <T>  The type of the Java configuration bean.
     * @return An instance of the requested type or {@code null} if it couldn't be retrieved.
     */
    <T> T get(Class<T> type);

    /**
     * Retrieve Java class of a certain type for the given key from the cluster configuration.
     *
     * @param key  The key that is used to find the cluster config object in the database.
     * @param type The {@link Class} of the Java configuration bean to retrieve.
     * @param <T>  The type of the Java configuration bean.
     * @return An instance of the requested type or {@code null} if it couldn't be retrieved.
     */
    <T> T get(String key, Class<T> type);

    /**
     * Retrieve Java class of a certain type from the cluster configuration or return a default value
     * in case that failed.
     *
     * @param type         The {@link Class} of the Java configuration bean to retrieve.
     * @param defaultValue An instance of {@code T} which is returned as default value.
     * @param <T>          The type of the Java configuration bean.
     * @return An instance of the requested type.
     */
    <T> T getOrDefault(Class<T> type, T defaultValue);

    /**
     * Write a configuration bean to the cluster configuration with the specified key.
     * @param key     The key that is used to write the cluster config object to the database.
     * @param payload The object to write to the cluster configuration. Must be serializable by Jackson!
     * @param <T>     The type of the Java configuration bean.
     */
    <T> void write(String key, T payload);

    /**
     * Write a configuration bean to the cluster configuration.
     *
     * @param payload The object to write to the cluster configuration. Must be serializable by Jackson!
     * @param <T>     The type of the Java configuration bean.
     */
    <T> void write(T payload);

    /**
     * Remove a configuration bean from the cluster configuration.
     *
     * @param type The {@link Class} of the Java configuration bean to remove.
     * @return The number of removed entries from the cluster configuration.
     */
    <T> int remove(Class<T> type);

    /**
     * List all classes of configuration beans in the database.
     *
     * @return The list of Java classes being used in the database.
     */
    Set<Class<?>> list();
}
