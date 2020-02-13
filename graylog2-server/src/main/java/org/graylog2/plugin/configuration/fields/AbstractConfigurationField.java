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

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class AbstractConfigurationField implements ConfigurationField {
    protected final String field_type;
    protected final String name;
    protected final String humanName;
    protected final String description;
    protected final ConfigurationField.Optional optional;
    protected int position;
    final int DEFAULT_POSITION = 100;

    public AbstractConfigurationField(String field_type, String name, String humanName, String description, ConfigurationField.Optional optional1) {
        this.field_type = field_type;
        this.name = name;
        this.humanName = humanName;
        this.description = description;
        this.optional = optional1;
        this.position = DEFAULT_POSITION;
    }
    public AbstractConfigurationField(String field_type, String name, String humanName, String description, ConfigurationField.Optional optional1, int position) {
        this(field_type, name, humanName,description,optional1);
        this.position = position;
    }

    @Override
    public String getFieldType() {
        return field_type;
    }

    @Override
    public ConfigurationField.Optional isOptional() {
        return optional;
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
    public List<String> getAttributes() {
        return Collections.emptyList();
    }

    @Override
    public Map<String, Map<String, String>> getAdditionalInformation() {
        return Collections.emptyMap();
    }

    @Override
    public int getPosition() {
        return position;
    }
}
