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
package org.graylog2.restclient.lib.plugin.configuration;

import java.util.Map;

public class DropdownField extends RequestedConfigurationField {

    private final static String TYPE = "dropdown";

    private final Map<String, String> values;

    public DropdownField(Map.Entry<String, Map<String, Object>> c) {
        super(TYPE, c);

        // lolwut
        this.values = (Map<String, String>) ((Map<String, Object>) c.getValue().get("additional_info")).get("values");
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String attributeToJSValidation(String attribute) {
        throw new RuntimeException("This type does not have any validatable attributes.");
    }

    public Map<String, String> getValues() {
        return values;
    }
}
