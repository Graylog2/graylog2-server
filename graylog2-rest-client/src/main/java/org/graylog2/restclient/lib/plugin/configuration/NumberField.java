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

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class NumberField extends RequestedConfigurationField {

    private final static String TYPE = "number";

    public enum Attribute {
        ONLY_POSITIVE,
        ONLY_NEGATIVE,
        IS_PORT_NUMBER
    }

    public NumberField(Map.Entry<String, Map<String, Object>> c) {
        super(TYPE, c);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String attributeToJSValidation(String attribute) {
        switch (Attribute.valueOf(attribute.toUpperCase())) {
            case ONLY_NEGATIVE:
                return "negative_number";
            case ONLY_POSITIVE:
                return "positive_number";
            case IS_PORT_NUMBER:
                return "port_number";
            default:
                throw new RuntimeException("No JS validation for type [" + attribute + "].");
        }
    }

}
