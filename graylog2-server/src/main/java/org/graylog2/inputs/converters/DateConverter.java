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
package org.graylog2.inputs.converters;

import org.graylog2.ConfigurationException;
import org.graylog2.plugin.inputs.Converter;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class DateConverter extends Converter {

    private final String dateFormat;

    public DateConverter(Map<String, Object> config) throws ConfigurationException {
        super(Type.DATE, config);

        if (config.get("date_format") == null || ((String) config.get("date_format")).isEmpty()) {
            throw new ConfigurationException("Missing config [date_format].");
        }

        dateFormat = (String) config.get("date_format");
    }

    @Override
    public Object convert(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        return DateTime.parse(value, DateTimeFormat.forPattern(dateFormat));
    }

    @Override
    public boolean buildsMultipleFields() {
        return false;
    }

}
