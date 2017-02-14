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
package org.graylog2.plugin.configuration.fields;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ListField extends AbstractConfigurationField {
    public static final String FIELD_TYPE = "list";

    public enum Attribute {
        ALLOW_CREATE,
    }

    private List<String> defaultValue;
    private Map<String, String> values;
    private List<String> attributes;

    public ListField(String name, String humanName, List<String> defaultValue, String description, Optional isOptional) {
        this(name, humanName, defaultValue, Collections.emptyMap(), description, isOptional);
    }

    public ListField(String name, String humanName, List<String> defaultValue, Map<String, String> values, String description, Optional isOptional, Attribute... attributes) {
        super(FIELD_TYPE, name, humanName, description, isOptional);
        this.defaultValue = defaultValue;
        this.values = values;
        this.attributes = Arrays.stream(attributes)
                .map(attribute -> attribute.toString().toLowerCase(Locale.ENGLISH))
                .collect(Collectors.toList());
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        if (defaultValue instanceof List) {
            final List<?> defaultValueList = (List<?>) defaultValue;
            this.defaultValue = defaultValueList.stream()
                    .filter(o -> o instanceof String)
                    .map(String::valueOf)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public Map<String, Map<String, String>> getAdditionalInformation() {
        return Collections.singletonMap("values", values);
    }

    @Override
    public List<String> getAttributes() {
        return attributes;
    }
}
