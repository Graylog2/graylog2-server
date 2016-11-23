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
package org.graylog2.utilities;

import com.google.common.collect.Maps;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.database.ValidationException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigurationMapConverter {
    /**
     * Converts the values in the map to the requested types. This has been copied from the Graylog web interface
     * and should be removed once we have better configuration objects.
     */
    public static Map<String, Object> convertValues(final Map<String, Object> data, final ConfigurationRequest configurationRequest) throws ValidationException {
        final Map<String, Object> configuration = Maps.newHashMapWithExpectedSize(data.size());
        final Map<String, Map<String, Object>> configurationFields = configurationRequest.asList();

        for (final Map.Entry<String, Object> entry : data.entrySet()) {
            final String field = entry.getKey();
            final Map<String, Object> fieldDescription = configurationFields.get(field);
            if (fieldDescription == null || fieldDescription.isEmpty()) {
                throw new ValidationException(field, "Unknown configuration field description for field \"" + field + "\"");
            }

            final String type = (String) fieldDescription.get("type");

            // Decide what to cast to. (string, bool, number)
            Object value;
            switch (type) {
                case "text":
                case "dropdown":
                    value = entry.getValue() == null ? "" : String.valueOf(entry.getValue());
                    break;
                case "number":
                    try {
                        value = Integer.parseInt(String.valueOf(entry.getValue()));
                    } catch (NumberFormatException e) {
                        // If a numeric field is optional and not provided, use null as value
                        if ("true".equals(String.valueOf(fieldDescription.get("is_optional")))) {
                            value = null;
                        } else {
                            throw new ValidationException(field, e.getMessage());
                        }
                    }
                    break;
                case "boolean":
                    value = "true".equalsIgnoreCase(String.valueOf(entry.getValue()));
                    break;
                case "list":
                    final List<?> valueList = entry.getValue() == null ? Collections.emptyList() : (List<?>) entry.getValue();
                    value = valueList.stream()
                            .filter(o -> o != null && o instanceof String)
                            .map(String::valueOf)
                            .collect(Collectors.toList());
                    break;
                default:
                    throw new ValidationException(field, "Unknown configuration field type \"" + type + "\"");
            }

            configuration.put(field, value);
        }

        return configuration;
    }
}
