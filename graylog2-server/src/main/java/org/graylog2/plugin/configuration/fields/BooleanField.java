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

public class BooleanField extends AbstractConfigurationField {

    public static final String FIELD_TYPE = "boolean";

    private boolean defaultValue;

    public BooleanField(String name, String humanName, boolean defaultValue, String description) {
        super(FIELD_TYPE, name, humanName, description, Optional.OPTIONAL);
        this.defaultValue = defaultValue;
    }
    public BooleanField(String name, String humanName, boolean defaultValue, String description, int position) {
        super(FIELD_TYPE, name, humanName, description, Optional.OPTIONAL, position);
        this.defaultValue = defaultValue;
    }

    @Override
    public Object getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        if (defaultValue instanceof Boolean) {
            this.defaultValue = (boolean) defaultValue;
        }
    }
}
