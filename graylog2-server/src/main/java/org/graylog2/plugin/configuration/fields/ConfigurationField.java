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

import java.util.List;
import java.util.Map;

public interface ConfigurationField {
    int DEFAULT_POSITION = 100;  // corresponds to ConfigurationForm.jsx
    int PLACE_AT_END_POSITION = 200;

    enum Optional {
        OPTIONAL,
        NOT_OPTIONAL
    }

    String getFieldType();

    String getName();

    String getHumanName();

    String getDescription();

    Object getDefaultValue();

    void setDefaultValue(Object defaultValue);

    Optional isOptional();

    List<String> getAttributes();

    Map<String, Map<String, String>> getAdditionalInformation();

    default int getPosition() {
        return DEFAULT_POSITION;
    }
}
