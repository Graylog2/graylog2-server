/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package org.graylog2.plugin.configuration.fields;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class DropdownField implements ConfigurationField {

    public static final String FIELD_TYPE = "dropdown";

    private final String name;
    private final String humanName;
    private final String defaultValue;
    private final String description;
    private final Optional optional;

    public DropdownField(String name, String humanName, String defaultValue, Map<String, String> values, Optional isOptional) {
        this(name, humanName, defaultValue, values, null, isOptional);
    }

    public DropdownField(String name, String humanName, String defaultValue, Map<String, String> values, String description, Optional isOptional) {
        this.name = name;
        this.humanName = humanName;
        this.defaultValue = defaultValue;
        this.description = description;
        this.optional = isOptional;
    }

    @Override
    public String getFieldType() {
        return FIELD_TYPE;
    }
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getHumanName() {
        return humanName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public Optional isOptional() {
        return optional;
    }

    @Override
    public List<String> getAttributes() {
        return Lists.newArrayList();
    }

    public static class ValueTemplates {

        public static Map<String, String> timeUnits() {
            Map<String, String> units = Maps.newHashMap();

            for(TimeUnit unit : TimeUnit.values()) {
                units.put(unit.toString(), unit.toString().toLowerCase());
            }

            return units;
        }

    }

}