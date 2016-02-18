/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.plugin.cluster;

import java.util.Optional;
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
     * @return An instance of the requested type wrapped in {@link Optional}.
     */
    <T> Optional<T> get(Class<T> type);

    /**
     * Retrieve Java class of a certain type from the cluster configuration or return the default value
     * of the configuration class using a static method with signature {@code public static <T> T defaultConfig()}
     * in case it wasn't found.
     *
     * @param type The {@link Class} of the Java configuration bean to retrieve.
     * @param <T>  The type of the Java configuration bean.
     * @return An instance of the requested type wrapped in {@link Optional}.
     */
    <T> Optional<T> getOrDefault(Class<T> type);

    /**
     * Return the default config instance of a cluster config class using a static method with signature
     * {@code public static <T> T defaultConfig()}.
     *
     * @param type The {@link Class} of the Java configuration bean to retrieve.
     * @param <T>  The type of the Java configuration bean.
     * @return An instance of the requested type wrapped in {@link Optional}.
     */
    <T> Optional<T> getDefault(Class<T> type);

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
