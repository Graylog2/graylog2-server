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

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Locale;

public class NumberField extends AbstractConfigurationField {

    public static final String FIELD_TYPE = "number";

    public enum Attribute {
        ONLY_POSITIVE,
        ONLY_NEGATIVE,
        IS_PORT_NUMBER
    }

    private Number defaultValue;

    private final List<String> attributes;

    public NumberField(String name, String humanName, Number defaultValue, String description, Optional isOptional) {
        this(name, humanName, defaultValue, description, isOptional, new Attribute[0]);
    }

    public NumberField(String name, String humanName, Number defaultValue, String description, Attribute... attributes) {
        this(name, humanName, defaultValue, description, Optional.NOT_OPTIONAL, attributes);
    }

    public NumberField(String name, String humanName, Number defaultValue, String description, Optional isOptional, Attribute... attrs) {
        super(FIELD_TYPE, name, humanName, description, isOptional);
        this.defaultValue = defaultValue;

        this.attributes = Lists.newArrayList();
        if (attrs != null) {
            for (Attribute attribute : attrs) {
                this.attributes.add(attribute.toString().toLowerCase(Locale.ENGLISH));
            }
        }
    }

    @Override
    public Object getDefaultValue() {
        return defaultValue;
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        if (defaultValue instanceof Number) {
            this.defaultValue = (Number) defaultValue;
        }
    }

    @Override
    public List<String> getAttributes() {
        return attributes;
    }
}
