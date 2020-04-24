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

import java.util.Map;

public class RadioField extends AbstractChoiceField {

    public static final String FIELD_TYPE = "radio";

    public RadioField(String name, String humanName, String defaultValue, Map<String, String> values, Optional isOptional) {
        super(FIELD_TYPE, name, humanName, defaultValue, values, isOptional);
    }

    public RadioField(String name, String humanName, String defaultValue, Map<String, String> values, String description, Optional isOptional) {
        super(FIELD_TYPE, name, humanName, defaultValue, values, description, isOptional);
    }

    public RadioField(String name, String humanName, String defaultValue, Map<String, String> values, String description, Optional isOptional, int position) {
        super(FIELD_TYPE, name, humanName, defaultValue, values, description, isOptional, position);
    }
}
