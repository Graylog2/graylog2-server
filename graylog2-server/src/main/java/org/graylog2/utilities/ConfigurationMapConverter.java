/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.utilities;

import com.google.common.collect.Maps;
import org.graylog2.database.ValidationException;
import org.graylog2.plugin.configuration.ConfigurationRequest;

import java.util.Map;

public class ConfigurationMapConverter {
    /**
     * Converts the values in the map to the requested types. This has been copied from the Graylog2 web interface
     * and should be removed once we have better configuration objects.
     */
    public static Map<String, Object> convertValues(final Map<String, Object> data, final ConfigurationRequest configurationRequest) throws ValidationException {
        final Map<String, Object> configuration = Maps.newHashMapWithExpectedSize(data.size());
        final Map<String, Map<String, Object>> configurationFields = configurationRequest.asList();

        for (final Map.Entry<String, Object> entry : data.entrySet()) {
            final Object value;
            // Decide what to cast to. (string, bool, number)
            switch ((String) configurationFields.get(entry.getKey()).get("type")) {
                case "text":
                    value = String.valueOf(entry.getValue());
                    break;
                case "number":
                    try {
                        value = Integer.parseInt(String.valueOf(entry.getValue()));
                    } catch (NumberFormatException e) {
                        throw new ValidationException(entry.getKey(), e.getMessage());
                    }
                    break;
                case "boolean":
                    value = "true".equals(String.valueOf(entry.getValue()));
                    break;
                case "dropdown":
                    value = String.valueOf(entry.getValue());
                    break;
                default:
                    value = entry.getValue();
            }

            configuration.put(entry.getKey(), value);
        }

        return configuration;
    }
}
