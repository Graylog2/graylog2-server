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

import com.google.common.collect.Maps;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class AbstractChoiceField extends AbstractConfigurationField {

    private String defaultValue;
    private final Map<String, String> values;

    public AbstractChoiceField(String type, String name, String humanName, String defaultValue, Map<String, String> values, Optional isOptional) {
        this(type, name, humanName, defaultValue, values, null, isOptional);
    }

    public AbstractChoiceField(String type, String name, String humanName, String defaultValue, Map<String, String> values, String description, Optional isOptional) {
        super(type, name, humanName, description, isOptional);
        this.defaultValue = defaultValue;
        this.values = values;
    }

    public AbstractChoiceField(String type, String name, String humanName, String defaultValue, Map<String, String> values, String description, Optional isOptional, int position) {
        this(type, name, humanName, defaultValue, values, description, isOptional);
        this.position = position;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        if (defaultValue instanceof String) {
            this.defaultValue = (String) defaultValue;
        }
    }

    @Override
    public Map<String, Map<String, String>> getAdditionalInformation() {
        Map<String, Map<String, String>> result = Maps.newHashMap();
        result.put("values", values);
        return result;
    }

    public static class ValueTemplates {

        public static Map<String, String> timeUnits() {
            Map<String, String> units = Maps.newHashMap();

            for (TimeUnit unit : TimeUnit.values()) {
                String human = unit.toString().toLowerCase(Locale.ENGLISH);
                units.put(unit.toString(), Character.toUpperCase(human.charAt(0)) + human.substring(1));
            }

            return units;
        }

        public static Map<String, String> valueMapFromEnum(Class<? extends Enum> enumClass, Function<Enum, String> valueMapping) {
            return Arrays.stream(enumClass.getEnumConstants()).collect(Collectors.toMap(Enum::toString, valueMapping));
        }

    }
}
