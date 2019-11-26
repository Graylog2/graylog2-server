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
package org.graylog.testing;

import com.google.common.io.Resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Properties;

/**
 * Loads properties from a resource.
 */
public class PropertyLoader {
    /**
     * Load the properties from the given resource. Returns an {@link UncheckedIOException} when loading the resources
     * failed.
     *
     * @param resourcePath the resource path
     * @return loaded properties
     */
    public static Properties loadProperties(String resourcePath) {
        final Properties properties = new Properties();
        final URL resource = Resources.getResource(resourcePath);
        try (InputStream stream = resource.openStream()) {
            properties.load(stream);
        } catch (IOException e) {
            throw new UncheckedIOException("Error while reading test properties " + resourcePath, e);
        }
        return properties;
    }
}
